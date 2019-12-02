package conn;

import json.config.Config;
import json.Message;

import java.util.List;

public interface Channel {

    /**
     * 初始化管道
     * @param config
     */
    void initConfig(Config config) throws Exception;

    /**
     * 将消息发送给指定目标
     * @param message
     * @param targetId
     */
    void send(Message message, int targetId) throws Exception;

    /**
     * 获取缓存中的所有消息
     * @return 消息列表
     */
    List<Message> recv() throws Exception;

    /**
     * 获取当前节点编号
     * @return 获取当前节点编号
     */
    int getId();
}
