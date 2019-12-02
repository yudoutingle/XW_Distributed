import com.fasterxml.jackson.databind.ObjectMapper;
import json.Message;

public class Const {
    public final static String CALL_TYPE_PREPARE = "prepare";
    public final static String CALL_TYPE_SEND = "send";
    public final static String CALL_TYPE_SYS = "sys";
    public final static String CALL_TYPE_CHANNEL_BUILD = "channel_build";
    public final static String CALL_TYPE_CHANNEL_DESTROY = "channel_destroy";

    public final static int STATE_REQUEST = 0;
    public final static int STATE_ACCEPT = 1;
    public final static int STATE_REFUSE = 2;
    public final static int STATE_NOTICE = -1;

    public final static int CHANNEL_TYPE_ERROR = -1;
    public final static int CHANNEL_TYPE_NORMAL = 0;
    public final static int CHANNEL_TYPE_FAST = 1;

    public final static int ERR_CODE_NONE = 0x0;
    public final static int ERR_CODE_NO_SUCH_CHANNEL = 0x001;
    public final static int ERR_CODE_NO_SUCH_CALL_TYPE = 0x002;
    public final static int ERR_CODE_CHANNEL_BUILD_MASK = 0x0100;
    public final static int ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE = 0x0101;
    public final static int ERR_CODE_CHANNEL_BUILD_TARGET_LIMIT = 0x0102;
    public final static int ERR_CODE_CHANNEL_BUILD_TOTAL_LIMIT = 0x0103;
    public final static int ERR_CODE_CHANNEL_BUILD_SOURCE_LIMIT = 0x0104;
    public final static int ERR_CODE_CHANNEL_BUILD_TARGET_TIMEOUT = 0x0105;
    public final static int ERR_CODE_CHANNEL_BUILD_UNKNOWN_OPERATION = 0x106;
    public final static int ERR_CODE_SEND_MASK = 0x200;
    public final static int ERR_CODE_SEND_COUNT_LIMIT = 0x201;
    public final static int ERR_CODE_SEND_SIZE_LIMIT = 0x202;

    public final static double EXP = 1e-5;

    public final static ObjectMapper MAPPER = new ObjectMapper();
    public final static String EMPTY_MESSAGE = "{\"callType\": \"\",\"channelId\": 0,\"sysMessage\": {\"target\": 0,\"data\": \"\",\"delay\": 0.0},\"extMessage\": {},\"state\": 0,\"errCode\": 0,\"channelType\": 0}";
    public static Message GetEmptyMessage() {
        try {
            return MAPPER.readValue(EMPTY_MESSAGE, Message.class);
        } catch (Exception e) {
            return null;
        }
    }

    public final static int CHANNEL_STATE_REFUSE = 0;
    public final static int CHANNEL_STATE_REQUEST = 1;
    public final static int CHANNEL_STATE_ACCEPT = 2;
    public final static int CHANNEL_STATE_SUCCESS = 3;
    public final static int CHANNEL_STATE_FAILED = 4;
    public final static int CHANNEL_STATE_NONE = 5;
}
