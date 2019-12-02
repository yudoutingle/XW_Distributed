package json;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Message implements Cloneable{
    public String callType;
    public int channelId;
    public SysMessage sysMessage;
    public Map<String, String> extMessage;
    public int state;
    public int errCode;
    public int channelType;
    public int targetId;
    @JsonIgnore
    public double recvTime;
    //添加到队列中的时间 
    public double sendTime;
    
    /**
     * recv time 是本地 初始化的时间 或 从json包提取出来的时间。
     */
    public Message() {
        recvTime = new Date().getTime() / 1000.0;
        extMessage = new HashMap<String, String>();
        sendTime = recvTime;
    }

    @Override
    public Message clone() throws CloneNotSupportedException {
    	
    	Message m = (Message) super.clone();
    	m.extMessage = new HashMap<String, String>();
    	for(String key:this.extMessage.keySet()){
    		m.extMessage.put(key, this.extMessage.get(key));
    	}
    	m.sysMessage = this.sysMessage.clone();
        return m;
    }
    
    public void setsendTime() {
    	sendTime = new Date().getTime() / 1000.0;
    }
    
    public String toString() {
    	return this.sysMessage.target + " : " + this.sysMessage.data;
    }
}
