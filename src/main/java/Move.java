import json.Message;
import json.config.ChannelDetialConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Move {
    private int target;
    private double timeout;
    private int channelState, waitingCount;
    private List<Message> queue;
    private Control control;
    private int channelType;
    private int channelId;

    public Move(int target, Control control) {
        this.target = target;
        this.control = control;
        timeout = 0;
        waitingCount = 0;
        queue = new ArrayList<>();
        clearChannelInfo();
    }

    private void clearChannelInfo() {
        channelType = Const.CHANNEL_TYPE_ERROR;
        channelState = Const.CHANNEL_STATE_NONE;
        channelId = 0;
    }

    public boolean isValid() {
        return channelType != Const.CHANNEL_TYPE_ERROR && channelState == Const.CHANNEL_STATE_SUCCESS;
    }

    public int getOtherType() {
        double high=2*Main.config.channelConfig.highSpeed.buildTime+Main.config.channelConfig.highSpeed.lag;
        double normal=2*Main.config.channelConfig.normalSpeed.buildTime+Main.config.channelConfig.normalSpeed.lag;
//        System.out.println("high:"+high+"      normal:"+normal);
        if(high>normal){
            return Const.CHANNEL_TYPE_NORMAL;
        }
        return Const.CHANNEL_TYPE_FAST;
    }

    public void doRequest(int channelType) {
        if (this.channelType != Const.CHANNEL_TYPE_ERROR) return ;
        this.channelType = channelType;
        channelState = Const.CHANNEL_STATE_REQUEST;
        control.sendChannelBuild(target, Const.STATE_REQUEST, Const.ERR_CODE_NONE, channelType);
    }

    public void onRequest(Message message) {
        int target = message.sysMessage.target;
        if (channelState != Const.CHANNEL_STATE_NONE) {
            if (control.getId() < target) {
                control.sendChannelBuild(target, Const.STATE_REFUSE,
                        Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE, message.channelType);
                return ;
            }
        }
        control.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
        channelType = message.channelType;
        channelState = Const.CHANNEL_STATE_ACCEPT;
    }

    public void onSucc(Message message) {
        channelType = message.channelType;
        channelState = Const.CHANNEL_STATE_SUCCESS;
        channelId = message.channelId;
        filterQueue();
        queue.forEach(msg -> doSend(msg));
        queue.clear();
    }

    public void onRefuse(Message message) {
        if (channelState != Const.CHANNEL_STATE_SUCCESS) {
//            System.out.println("on refuse");
            int next = getOtherType();
            clearChannelInfo();
            filterQueue();
            if (waitingCount > 0 || queue.size() > 0) {
                doRequest(next);
            }
        }
    }

    public void onDestroy(Message message) {
        if (channelState == Const.CHANNEL_STATE_SUCCESS) {
//            System.out.println("on destroy");
            clearChannelInfo();
        }
        filterQueue();
        if (waitingCount > 0 || queue.size() > 0) {
            doRequest(getOtherType());
        }
    }

    public void onPrepare(Message message) {
        timeout = Math.max(timeout, Main.curTime() + Main.config.mainConfig.timeOut);
        waitingCount++;
        if (channelState == Const.CHANNEL_STATE_NONE) {
            doRequest(getOtherType());
        }
    }

    public void onSend(Message message) {
        if (control.getId() == message.sysMessage.target) {
//            System.out.println("succ received message: " + message.sysMessage.data);
            Message message1=Const.GetEmptyMessage();
            message1.callType=Const.CALL_TYPE_CHANNEL_DESTROY;
            message1.channelId=message.channelId;
            message1.state= Const.STATE_REQUEST;
            message1.errCode=Const.ERR_CODE_NONE;
            control.doSend(message1,0);
            return ;
        }
        waitingCount--;
        if (channelState == Const.CHANNEL_STATE_SUCCESS) {
//            System.out.println("send directory");
            doSend(message);
        } else {
//            System.out.println("add into cache");
//            System.out.printf("channelState:%d\n", channelState);
            queue.add(message);
        }
    }

    public void doSend(Message message) {
        if (message.recvTime + Main.config.mainConfig.timeOut >= Main.curTime() + getConfig().lag) {
            message.channelId = channelId;
            control.doSend(message, message.sysMessage.target);
        }
    }

    public void filterQueue() {
        ArrayList<Message> filtered = new ArrayList<>();
        for (Message message : queue) {
            ChannelDetialConfig selfConf = getConfig();
            float lag = selfConf == null ? 0 : selfConf.lag;
            if (message.recvTime + Main.config.mainConfig.timeOut >= Main.curTime() + lag) {
                filtered.add(message);
            }
        }
        queue = filtered;
    }

    public ChannelDetialConfig getConfig() {
        switch (channelType) {
            case Const.CHANNEL_TYPE_FAST :
                return Main.config.channelConfig.highSpeed;
            case Const.CHANNEL_TYPE_NORMAL:
                return Main.config.channelConfig.normalSpeed;
            default:
                return null;
        }
    }
}
