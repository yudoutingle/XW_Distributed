import java.util.*;

import conn.Channel;
import json.Message;
import json.config.Config;
/**
 * 只能做 AP的， 不能保证 数据 一致。
 * @author 雨都停了。
 *
 */
public class Node {
	// node id 
	public int id;
	// node 的最大连接数
	private final int maxChannelConn;
	// node 连接的 节点及对应的通道
	private Map<Integer, Integer> channelMap;
	//channel state map
	private Map<Integer, Integer> stateMap;
	// 与 master 的 channel 
	private Channel channel;	

	// 存放还没发送出去的send消息。
	private Map<Integer, List<Message>> waitqueue = new HashMap<Integer, List<Message>>();
	
	private Config config;
	
	public int N ;
	
	private int leftchild, rightchild;
	
	private int father;
		
	private boolean initFlag = false;

	private double[][] graph ;
	
	private double normalLag; 
	private double fastlLag; 
	
	private int nowconn = 0;
	/**
	 * 新加的代码部分
	 * @param chan
	 * @param config
	 */
	//邻居的map
	private Map<Integer, List<Integer>> neighbours_map = new HashMap<>(); 
	private Map<Integer, Integer> request_map = new HashMap<Integer, Integer>(); 
	//
	private Set<Integer> refuseset = new HashSet<Integer>();
	private Set<Integer> neighbours = new HashSet<Integer>();
	
	private boolean ifFull = false;
	
	
	public Node(Channel chan, Config config) {
		this.channel = chan;
		channelMap = new HashMap<>();
		stateMap = new HashMap< >();
		this.config = config;
		maxChannelConn = this.config.maxChannelConn;
		id = chan.getId();
		//总结点数
		N = config.mainConfig.nodeCount;
		//树形的层数
		
		leftchild = (2 * this.id) <= N? 2 * this.id : 0;
		rightchild = (2 * this.id + 1) <= N? 2 * this.id + 1: 0;
		
		this.father = (this.id == 1) ? 0 : this.id/2;

		fastlLag = config.channelConfig.highSpeed.lag;
		normalLag = config.channelConfig.normalSpeed.lag;
		//网络 唤醒
		notice();
		//初始化图
		initGraph();

	}
	
	
	//完全二叉树 初始化。
	public void init() {
		if(!this.initFlag) {			
			this.initFlag = true;
			
			int type = this.id *2 < config.channelConfig.highSpeed.maxCount ? 1 : 0;
			System.out.println("id : " + this.id  +  " N  : "+ N + " left : " + leftchild + " right " + rightchild);
			if(leftchild!=0) {
				send_build(leftchild, Const.STATE_REQUEST, Const.STATE_NOTICE, type);
				this.stateMap.put(leftchild, Const.CHANNEL_STATE_REQUEST);
				this.request_map.put(leftchild, type);
			}
			if(rightchild != 0) {
				send_build(rightchild, Const.STATE_REQUEST, Const.STATE_NOTICE, type);
				this.stateMap.put(rightchild, Const.CHANNEL_STATE_REQUEST);
				this.request_map.put(rightchild, type);
			}
//			send_build(this.id/2, Const.STATE_REQUEST, Const.STATE_NOTICE, (this.id-1) < config.channelConfig.highSpeed.maxCount ? 1 : 0);
//			this.stateMap.put(this.id/2, Const.CHANNEL_STATE_REQUEST);
//			this.request_map.put(this.id/2, (this.id-1) < config.channelConfig.highSpeed.maxCount ? 1 : 0);
		}
	}
	
	private void notice() {
		if(this.id == N) {
//			this.initFlag = true;
			for(int i = 1; i<N; i++) {
//				if(i == N/2)
//					send_build(i,Const.STATE_REQUEST, Const.STATE_NOTICE, 1);
//				else
				send_build(i,Const.STATE_REQUEST, Const.STATE_NOTICE, Const.CHANNEL_TYPE_NORMAL);
			}
		}
	}
	
	/**
	 * 初始化图
	 */
	public void initGraph() {
		this.graph = Search_Path.initG(N, 0);
		for(int i = 2 ; i<=N ;i ++) {
			Search_Path.add_adj(this.graph, i, i/2, i-1 < config.channelConfig.highSpeed.maxCount ? fastlLag : normalLag);
		}
	}
	

	
	
	public void recv(Message message) {
		
		System.out.println("NODE :     " + this.id);

		this.init();
		
		if(message.errCode != Const.ERR_CODE_NONE) {
			int err =message.errCode;
			//我方满、 对方满、目标超时了、
			if(err == Const.ERR_CODE_CHANNEL_BUILD_SOURCE_LIMIT || err == Const.ERR_CODE_CHANNEL_BUILD_TARGET_LIMIT || err == Const.ERR_CODE_CHANNEL_BUILD_TARGET_TIMEOUT) {
				// 以后这个 点 不会再连了。
				stateMap.put(message.sysMessage.target, Const.CHANNEL_STATE_FAILED);
				this.refuseset.add(message.sysMessage.target);
			}
			
			if(err == Const.ERR_CODE_CHANNEL_BUILD_TOTAL_LIMIT) {
				System.out.println("高速通道满了，换低速通道了");
				message.errCode = Const.ERR_CODE_NONE;
				send_build(message.sysMessage.target, 0);
			}
			// 初始化时候 会走这个部分 。
			// 初始化的时候 对方会走 这个代码， 
			if(err == Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE) {
				System.out.println("对方拒绝了");
				message.errCode = Const.ERR_CODE_NONE;
				if(message.sysMessage.target != this.id)
					this.recv_build(message);
			}
			
			// 走这通道意味着  这个是 send 消息， 这时候 要重新发送，
			if(err == 1) {
				System.out.println("no such channel");
				message.errCode = Const.ERR_CODE_NONE;
				this.recv_send(message);
			}
			return;
		}
		
		//prepare 消息
		if(message.callType.equals(Const.CALL_TYPE_PREPARE)) {
			System.out.println("node 节点 开始处理 prepare消息");
			recv_prepare(message);
		}
		//send 消息
		if(message.callType.equals(Const.CALL_TYPE_SEND)) {
			System.out.println("node 节点 开始处理 send 消息");
			recv_send(message);
		}
		// destroy
		if(message.callType.equals(Const.CALL_TYPE_CHANNEL_DESTROY)) {
			System.out.println("node 节点 开始处理 destroy消息");
			recv_destroy(message);
		}
		// sys-自定义
		if(message.callType.equals(Const.CALL_TYPE_SYS)) {
			//暂定
			
		}
		// build 消息
		if(message.callType.equals(Const.CALL_TYPE_CHANNEL_BUILD)) {
			System.out.println("node 节点 开始处理  build 消息");
			recv_build(message);
		}
	}
	
	
	
	/**
	 * 收到 prepare 怎么办
	 * @param message
	 */
	public void recv_prepare(Message message) {
		
		int target = message.sysMessage.target;
		
		// 如果初始化完成了 ， 那么
		if(this.ifinitfinish()) {
			if( ! this.neighbours.contains(target) && !this.ifFull && !this.refuseset.contains(target) && !stateMap.containsKey(target)) {
				int type = 1;
				send_build(target, Const.STATE_REQUEST,
                        Const.STATE_NOTICE, type);
				stateMap.put(target, Const.CHANNEL_STATE_REQUEST);
				this.request_map.put(target, type);
			}
		}
	}
	
	
	
	/**
	 * @param message
	 */
	public void recv_send(Message message) {
		//这个就是目标节点
		System.out.println(this.channelMap);
		int target = message.sysMessage.target;
		
		if(message.extMessage.size()<1) {
			//第一次收到send消息
			message.extMessage.put("recvtime", message.recvTime+"");
		}
		
		if(this.id == target) {
			double cur = new Date().getTime() / 1000.0;
			double recv = Double.valueOf(message.extMessage.get("recvtime"));
			System.out.println("收到消息: 耗时  : " + (cur - recv));
			
			if(!channelMap.containsKey(target))
				return;
			if(!isChildorFather(target)) {
				int type = message.channelType;
				send_destroy(target, channelMap.get(target));
				System.out.println("销毁通道了@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				this.recv_destroy(message);
			}
			return;
		}
		if(this.channelMap.containsKey(target)) {
			
			System.out.println("send  消息 可以直达啦 啦啦啦 ");
			send(message, target, channelMap.get(target));
			
		}
		else {
			//收到的sned 消息的目的地 如果在建造中， 就加入到 缓存中， 同时路由
			if(stateMap.containsKey(target) && stateMap.get(target)!=Const.CHANNEL_STATE_FAILED) {
				System.out.println("有这个状态，但是通道没好，吧消息加入到缓存中了");
				if(waitqueue.containsKey(target)) {
					waitqueue.get(target).add(message);
				}else {
					List<Message> list = new ArrayList<>();
					list.add(message);
					waitqueue.put(target, list);
				}
				System.out.println(waitqueue);
			}
			send_to(message);
		}

	}
	
	/**
	 * 纯粹的路由消息，找最短路， 然后发送。
	 * @param message
	 */
	public void send_to(Message message) {
		int target = message.sysMessage.target;
		
		int[] path = Search_Path.getShortedPath(graph, this.id, target);
		int next = 0;
		if(path.length > 1)
			next = path[1];
		/**
		 *  太有道理了！！ 这部分代码的意思 是 初始化的路由还没好，这个时候
		 *  你要等初始化路友好。
		 */
		if( !this.neighbours.contains(next) && next != 0) {
			if(waitqueue.containsKey(next)) {
				waitqueue.get(next).add(message);
			}else {
				List<Message> list = new ArrayList<>();
				list.add(message);
				waitqueue.put(next, list);
			}
			System.out.println(waitqueue);
			next = 0;
		}
		
		if(next == 0)
			return;
		System.out.println("路由到下一跳咯咯咯咯");
		this.send(message, next, channelMap.get(next));
	}
	
	/**
	 * 摧毁通道 就直接摧毁。
	 * @param message
	 */
	public void recv_destroy(Message message) {

		int channelID = message.channelId;
		int id = -1;
		for(int target: this.channelMap.keySet()) {
			if(this.channelMap.get(target) == channelID) {
				this.channelMap.remove(target);
				id = target;
				break;
			}
		}
		
		if(id == -1)
			return;
		
		this.nowconn--;
		this.ifFull = (this.nowconn == this.maxChannelConn);
	
		Search_Path.remove(graph, this.id, id);
		
		if(this.channelMap.containsKey(id)) {
			channelMap.remove(id);
		}
		
		if(this.stateMap.containsKey(id)) {
			this.stateMap.remove(id);
		}
		
		if(this.waitqueue.containsKey(id)) {
			this.waitqueue.remove(id);
		}
		
		if(this.neighbours_map.containsKey(id)) {
			this.neighbours_map.remove(id);
		}
		
		if(this.request_map.containsKey(id)) {
			this.request_map.remove(id);
		}
		
		if(this.refuseset.contains(id)) {
			this.refuseset.remove(id);
		}
		if(this.neighbours.contains(id)) {
			this.neighbours.remove(id);
		}
	}
	
	
	/**
	 * @param message
	 */
	public void recv_build(Message message) {
		
		if (message.channelId != 0) {
			this.nowconn ++;
			this.neighbours.add(message.sysMessage.target);
			
			this.ifFull = (this.nowconn == this.maxChannelConn);
			
			System.out.println("channel 建立成功， channelID = " + message.channelId);
        	//建立成功。 在map里添加。
            int target = message.sysMessage.target;

            this.channelMap.put(target, message.channelId);
            
            this.stateMap.put(target, Const.CHANNEL_STATE_SUCCESS);
            
            double lag = message.channelType == 0? normalLag : fastlLag;
            //加边
            Search_Path.add_adj(this.graph, message.sysMessage.target, this.id, lag);
            
            List<Integer> list = new ArrayList<>();
            list.add(message.channelId);
            list.add(message.channelType);
            this.neighbours_map.put(target, list);

            
        } else {
        	//id不存在, 说明是notice消息，或者对方反馈的消息。
        	//a-b . 这是b
            if(message.state == Const.STATE_NOTICE) {
            	
            	int target = message.sysMessage.target;
            	
//            	if( !(target == this.leftchild || target == rightchild)) {
            	if( this.father != 0 && target!= this.father) {
            		
            		if(!this.ifinitfinish()) {
            			send_build(target, Const.STATE_REFUSE,
                                Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE, message.channelType);
            		}
            		else {
            			if(this.ifFull) {
            				
            				send_build(target, Const.STATE_REFUSE,
                                    Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE, message.channelType);
            				
            				stateMap.put(target, Const.CHANNEL_STATE_FAILED);
            			}else {
            			send_build(target, Const.STATE_ACCEPT,
                                    Const.ERR_CODE_NONE, message.channelType);
            			}
            		}
            	}else {
            		if(this.ifFull) {
        				
        				send_build(target, Const.STATE_REFUSE,
                                Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE, message.channelType);
        				
        				stateMap.put(target, Const.CHANNEL_STATE_FAILED);
        			}else {
        				send_build(target, Const.STATE_ACCEPT,
                                Const.ERR_CODE_NONE, message.channelType);
        			}
            		
            	}
           
            }
            
        	// 对方拒绝了我的链接。
            else if(message.state == Const.STATE_REFUSE) {
            	System.out.println(" 这个错误码 我还没处理：   " + message.errCode);
            	int key = message.sysMessage.target;
            	if(!this.refuseset.contains(key)) {
            		this.refuseset.add(key);
            		if(this.request_map.containsKey(key)) {
            			int type = request_map.get(key)== 0 ? 1 : 0;
            			send_build(key,Const.STATE_REQUEST, Const.ERR_CODE_NONE, type);
            			this.stateMap.put(key, Const.CHANNEL_STATE_REFUSE);
            		}else {
            			stateMap.put(key, Const.CHANNEL_STATE_FAILED);
            		}
            	}
            	else {
            		stateMap.put(key, Const.CHANNEL_STATE_FAILED);
            	}
            	
            }
        }
		
		handlewaitqueue();
	}
	
	
	
	/**
	 * 发送 build消息 
	 * @param target
	 * @param channelType
	 */
	public void send_build(int target, int channelType) {
		Message message = Const.GetEmptyMessage();
        message.callType = Const.CALL_TYPE_CHANNEL_BUILD;
        message.state = Const.STATE_REQUEST;
        message.errCode = Const.ERR_CODE_NONE;
        // 通 道  类型
        message.channelType = channelType;
        // 对面收到后要发往 这个地址
        message.sysMessage.target = target;
        send(message, target);
	}
	
	
	public void send_build(int target, int state, int err, int channelType) {
		Message message = Const.GetEmptyMessage();
        message.callType = Const.CALL_TYPE_CHANNEL_BUILD;
        message.state = state;
        message.errCode = err;
        // 通 道  类型
        message.channelType = channelType;
        // 对面收到后要发往 这个地址
        message.sysMessage.target = target;
        send(message, target);
	}
	
	/**
	 * 发送自定义消息
	 * @param target
	 * @param channelId
	 * @param map
	 */
	public void send_sys(int target, int channelId, Map<String, String> map) {
		Message message = Const.GetEmptyMessage();
		message.callType = Const.CALL_TYPE_SYS;
        message.errCode = Const.ERR_CODE_NONE;
        // 走这通道
        message.channelId = channelId;
        // 加到
        for(String key: map.keySet()) {
        	message.extMessage.put(key, map.get(key));
        }
        send(message, target);
	}
	
	/**
	 * 销毁通道 
	 * @param target
	 * @param channelId
	 */
	public void send_destroy(int target, int channelId) {
		Message message = Const.GetEmptyMessage();
		message.callType = Const.CALL_TYPE_CHANNEL_DESTROY;
        message.errCode = Const.ERR_CODE_NONE;
        message.channelId = channelId;
        send(message, target);
	}
	
	
	
	/**
	 * 发送给master的， 不走 节点之间 的通道 。  target节点 。
	 * @param message
	 * @param target
	 */
	public void send(Message message, int target) {
        try {
            channel.send(message, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * 发送给 target节点、走 某一个通道。
	 * @param message
	 * @param target
	 */
	public void send(Message message, int target, int channelID) {

//		System.out.println("channelID :  " +  channelID);
        try {
        	if(target == message.sysMessage.target) {
//        		System.out.println("要到终点了");
        		//是终点 。
        		double cur = new Date().getTime()/1000.0;
				//超时， 移除就ok
        		System.out.println(cur - Double.valueOf(message.extMessage.get("recvtime")) - config.mainConfig.timeOut);
//				if( cur > Double.valueOf(message.extMessage.get("recvtime")) + config.mainConfig.timeOut) {
//					System.out.println("这个消息真的超时了");
//				}
        	}
        	message.channelId = channelID;
            channel.send(message, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	
	public int log2(int n) {
		return (int)(Math.log(n)/Math.log(2));
	}
	
	
	public int route(int target) {
		
//		List<Integer> path = this.g.getShortestPath(this.id, target);
		int[] path = Search_Path.getShortedPath(this.graph, this.id, target);
		System.out.println(Arrays.toString(path));
		if(path.length == 0)
			return 0;
		return path[1];
	}
	
	/**
	 * 过滤 waitqueue的消息，然后 把不合适的 wait消息 处理掉，对这些不合理的消息， 要么路由，要么直达。
	 */
	public void handlewaitqueue() {
		
		for(int id: waitqueue.keySet()) {
			if(!this.neighbours.contains(id)) {
				Iterator<Message> iterator = waitqueue.get(id).iterator();
				while(iterator.hasNext()) {
					Message msg = iterator.next();
					double cur = new Date().getTime()/1000.0;
					//超时， 移除就ok
					if( cur > Double.valueOf(msg.extMessage.get("recvtime")) + config.mainConfig.timeOut) {
						System.out.println( " 有代码超时了  ");
						iterator.remove();
						continue;
					}
				}
			}
		}
		
		System.out.println(waitqueue);
		// 剩下的是 不超时的 代码
		Iterator<Integer> iditer = waitqueue.keySet().iterator();
		while(iditer.hasNext()) {
			int id  = iditer.next();
			// 对于已经建立好的  或者 一定不可能建立的 ，可以直接路由了。
			if(stateMap.containsKey(id) && (stateMap.get(id)==Const.CHANNEL_STATE_SUCCESS ||  stateMap.get(id)== Const.CHANNEL_STATE_FAILED)) {
				Iterator<Message> iterator = waitqueue.get(id).iterator();
				while(iterator.hasNext()) {
					Message msg = iterator.next();
					this.send_to(msg);
				}
				
			}
			iditer.remove();;
		}
	}
	

	/**
	 * 初始化 是否结束
	 * @return
	 */
	public boolean ifinitfinish() {
		
		if(this.leftchild!=0 && ! this.channelMap.containsKey(this.leftchild)) {
			return false;
		}
		if(this.rightchild!=0 && !this.channelMap.containsKey(this.rightchild)) {
			return false;
		}
		if(this.father!=0 && !this.channelMap.containsKey(this.father))
			return false;
		
		return true;
		
	}
	
	public boolean isChildorFather(int target) {
		if(target == leftchild || target == rightchild || target == father)
			return true;
		return false;
	}
	
}


