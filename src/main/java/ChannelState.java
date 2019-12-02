import java.util.*;

import json.Message;
public class ChannelState {
	
	//通道ID
	public int channelId = 0;
	//通道类型
	public int channelType = -1;
	//在发送的消息队列， 只有send 消息。
	public List<Message> queue = new ArrayList<>();
	//通道状态
	public int state = 0;
	
	public ChannelState() {
		
	}
	
	@Override
	public String toString() {
		
		return channelId + " " + channelType + " "  + state;
	}
}
