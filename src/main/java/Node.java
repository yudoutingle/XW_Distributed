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

	//
	private Set<Integer> refuseset = new HashSet<Integer>();
	
	public Node(Channel chan, Config config) {
		this.channel = chan;
		channelMap = new HashMap<>();
		stateMap = new HashMap< >();
		this.config = config;
		maxChannelConn = this.config.maxChannelConn;
		id = chan.getId();
		//总结点数
		N = config.mainConfig.nodeCount;
		
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
	
			if(leftchild!=0) {
				send_build(leftchild, Const.STATE_REQUEST, Const.STATE_NOTICE, type);
				this.stateMap.put(leftchild, Const.CHANNEL_STATE_REQUEST);
			}
			if(rightchild != 0) {
				send_build(rightchild, Const.STATE_REQUEST, Const.STATE_NOTICE, type);
				this.stateMap.put(rightchild, Const.CHANNEL_STATE_REQUEST);
			}
		}
	}
	
	private void notice() {
		if(this.id == N) {
			for(int i = 1; i<N; i++) {
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

		this.init();			
		//prepare 消息
		if(message.callType.equals(Const.CALL_TYPE_PREPARE)) {
//			System.out.println("node 节点 开始处理 prepare消息");
			recv_prepare(message);
		}
		//send 消息
		if(message.callType.equals(Const.CALL_TYPE_SEND)) {
//			System.out.println("node 节点 开始处理 send 消息");
			recv_send(message);
		}
		// destroy
		if(message.callType.equals(Const.CALL_TYPE_CHANNEL_DESTROY)) {
//			System.out.println("node 节点 开始处理 destroy消息");
			recv_destroy(message);
		}
		// sys-自定义
		if(message.callType.equals(Const.CALL_TYPE_SYS)) {
			//暂定
			
		}
		// build 消息
		if(message.callType.equals(Const.CALL_TYPE_CHANNEL_BUILD)) {
//			System.out.println("node 节点 开始处理  build 消息");
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
			if( !this.refuseset.contains(target) && !stateMap.containsKey(target)) {
				//默认发 快速消息
				send_build(target, Const.STATE_REQUEST,Const.STATE_NOTICE, 1);
				stateMap.put(target, Const.CHANNEL_STATE_REQUEST);
			}
		}
	}
	
	
	/**
	 * @param message
	 */
	public void recv_send(Message message) {	
		int target = message.sysMessage.target;	
		if(message.extMessage.size()<1) {
			//第一次收到send消息，记录时间。
			message.extMessage.put("recvtime", message.recvTime+"");
		}
		
		if(this.id == target) {
			double cur = new Date().getTime() / 1000.0;
			double recv = Double.valueOf(message.extMessage.get("recvtime"));
			System.out.println("收到消息: 耗时  : " + (cur - recv));
			
			if(!channelMap.containsKey(target))
				return;
			if(!isChildorFather(target)) {
				send_destroy(target, channelMap.get(target));
				this.recv_destroy(message);
			}
			return;
		}
		//能直达
		if(this.channelMap.containsKey(target)) {
			send(message, target, channelMap.get(target));			
		}
		else {
			//收到的sned 消息的目的地 如果在建造中， 就加入到 缓存中， 同时路由
			if(stateMap.containsKey(target) && stateMap.get(target)!=Const.CHANNEL_STATE_FAILED) {
//				System.out.println("有这个状态，但是通道没好，吧消息加入到缓存中了");
				if(waitqueue.containsKey(target)) {
					waitqueue.get(target).add(message);
				}else {
					List<Message> list = new ArrayList<>();
					list.add(message);
					waitqueue.put(target, list);
				}
			}
			route(message);
		}

	}
	
	/**
	 * 纯粹的路由消息，找最短路， 然后发送。
	 * @param message
	 */
	public void route(Message message) {
		int target = message.sysMessage.target;
		
		int[] path = Search_Path.getShortedPath(graph, this.id, target);
		int next = 0;
		if(path.length > 1)
			next = path[1];
		/**
		 *   初始化的路由还没好，这个时候
		 *  你要等初始化 好。
		 */
		if( !this.channelMap.containsKey(next) && next != 0) {
			if(waitqueue.containsKey(next)) {
				waitqueue.get(next).add(message);
			}else {
				List<Message> list = new ArrayList<>();
				list.add(message);
				waitqueue.put(next, list);
			}
			next = 0;
		}
		
		if(next == 0)
			return;
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
		if(this.refuseset.contains(id)) {
			this.refuseset.remove(id);
		}
	}
	
	
	/**
	 * @param message
	 */
	public void recv_build(Message message) {
		
		
		if(message.errCode != Const.ERR_CODE_NONE) {
			int err =message.errCode;
			//我方满、 对方满、目标超时了、
			if(err == Const.ERR_CODE_CHANNEL_BUILD_TARGET_LIMIT ) {
				// 以后这个 点 不会再连了。
				stateMap.put(message.sysMessage.target, Const.CHANNEL_STATE_FAILED);
				this.refuseset.add(message.sysMessage.target);
			}
			
			if(err == Const.ERR_CODE_CHANNEL_BUILD_TOTAL_LIMIT) {
				System.out.println("高速通道满了，换低速通道了");
				message.errCode = Const.ERR_CODE_NONE;
				send_build(message.sysMessage.target, 0);
				return;
			}

			return ;
		}
		
		if (message.channelId != 0) {
			
			System.out.println("channel 建立成功， channelID = " + message.channelId);
        	//建立成功。 在map里添加。
            int target = message.sysMessage.target;

            this.channelMap.put(target, message.channelId);
            
            this.stateMap.put(target, Const.CHANNEL_STATE_SUCCESS);
            
            double lag = message.channelType == 0? normalLag : fastlLag;
            //加边
            Search_Path.add_adj(this.graph, message.sysMessage.target, this.id, lag);        
        } else {
        	//id不存在, 说明是notice消息，或者对方反馈的消息。
        	//a-b . 这是b
            if(message.state == Const.STATE_NOTICE) {
            	
            	int target = message.sysMessage.target;
            	if( this.father != 0 && target!= this.father) {            		
            		if(!this.ifinitfinish()) 
            			send_build(target, Const.STATE_REFUSE,Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE, message.channelType);         		
            		else 
            			send_build(target, Const.STATE_ACCEPT,Const.ERR_CODE_NONE, message.channelType);
            	}
            	else 
        				send_build(target, Const.STATE_ACCEPT,Const.ERR_CODE_NONE, message.channelType);	          
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
        try {
        	message.channelId = channelID;
            channel.send(message, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	
	public int log2(int n) {
		return (int)(Math.log(n)/Math.log(2));
	}
	
	
	
	/**
	 * 过滤 waitqueue的消息，然后 把不合适的 wait消息 处理掉，对这些不合理的消息， 要么路由，要么直达。
	 */
	public void handlewaitqueue() {

		for(int id: waitqueue.keySet()) {
			if(!this.channelMap.containsKey(id)) {
				Iterator<Message> iterator = waitqueue.get(id).iterator();
				while(iterator.hasNext()) {
					Message msg = iterator.next();
					double cur = new Date().getTime()/1000.0;
					//超时， 移除就ok
					if( cur > Double.valueOf(msg.extMessage.get("recvtime")) + config.mainConfig.timeOut) {
						System.out.println("超时  : " + (cur - Double.valueOf(msg.extMessage.get("recvtime")) - config.mainConfig.timeOut) + " 移除了一个消息:  " + msg.sysMessage.data);
						iterator.remove();
						continue;
					}
				}
			}
		}
		
		Iterator<Integer> iditer = waitqueue.keySet().iterator();
		while(iditer.hasNext()) {
			int id  = iditer.next();
			boolean flag = false;
			// 对于已经建立好的  或者 一定不可能建立的 ，可以直接路由了。
			if(stateMap.containsKey(id) && (stateMap.get(id)==Const.CHANNEL_STATE_SUCCESS ||  stateMap.get(id)== Const.CHANNEL_STATE_FAILED)) {
				Iterator<Message> iterator = waitqueue.get(id).iterator();
				while(iterator.hasNext()) {
					Message msg = iterator.next();
					this.route(msg);
				}
				flag = true;
			}
			if(flag)
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


