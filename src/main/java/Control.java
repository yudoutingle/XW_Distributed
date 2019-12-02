import conn.Channel;
import json.Message;

import java.util.HashMap;
import java.util.Map;

public class Control {
    private Channel channel;
    Move[] move;
    Map<Integer, Integer> channelMap;

    public Control(Channel channel) {
        this.channel = channel;
        int N = Main.config.mainConfig.nodeCount;
        move = new Move[N+1];
        for (int i=1; i<=N; i++) {
            move[i] = new Move(i, this);
        }
        channelMap = new HashMap<>();
    }

    public int getId() {
        return channel.getId();
    }

    public void onRecv(Message message) {
        if (message.errCode != Const.ERR_CODE_NONE) {
            return ;
        }
        switch (message.callType) {
            case Const.CALL_TYPE_PREPARE:
                move[message.sysMessage.target].onPrepare(message);
                break;
            case Const.CALL_TYPE_SEND:
                move[message.sysMessage.target].onSend(message);
                break;
            case Const.CALL_TYPE_SYS:
                break;
            case Const.CALL_TYPE_CHANNEL_BUILD:
                if (message.channelId != 0) {
                    move[message.sysMessage.target].onSucc(message);
                    channelMap.put(message.channelId, message.sysMessage.target);
                } else {
                    switch (message.state) {
                        case Const.STATE_NOTICE:
                            move[message.sysMessage.target].onRequest(message);
                            break;
                        case Const.STATE_REFUSE:
                            move[message.sysMessage.target].onRefuse(message);
                            break;
                    }
                }
                break;
            case Const.CALL_TYPE_CHANNEL_DESTROY:
                int target = channelMap.getOrDefault(message.channelId, 0);
                if (target!=0) {
                    move[target].onDestroy(message);
                    channelMap.remove(message.channelId);
                }
                break;
        }
    }

    public void sendChannelBuild(int target, int state, int errCode, int channelType) {
        Message message = Const.GetEmptyMessage();
        message.callType = Const.CALL_TYPE_CHANNEL_BUILD;
        message.state = state;
        message.sysMessage.target = target;
        message.errCode = errCode;
        message.channelType = channelType;
        doSend(message, 0);
    }

    public void doSend(Message message, int target) {
        try {
            channel.send(message, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
