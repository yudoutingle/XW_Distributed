import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Hello world!
 *
 */

public class App {

	// 随机图 10
	private static final int randomGraph = 0;
	// 星型图 30
	private static final int starGraph = 1;
	// 分组图 30
	private static final int groupGraph = 2;
	// 二分图 30
	private static final int bipartiteGraph = 3;

	private static List<Integer> nodeMapping;



	public static void main(String[] args) throws Exception {
		String filePath = "C:\\Users\\雨都停了。\\Desktop\\分布式赛道\\data\\input10\\input10.json";
		byte[] bytes = FileUtils.readFileToByteArray(new File(filePath));
		JSONObject inputJSON = JSON.parseObject(new String(bytes, Charset.forName("utf-8")));

		JSONObject clientdata = generateClientConfData(inputJSON);
		JSONObject serverdata = generateServerConfData(inputJSON);
		JSONArray testData = generateTestDataJSON(inputJSON);

		FileUtils.writeByteArrayToFile(new File("C:\\Users\\雨都停了。\\Desktop\\分布式赛道\\data\\input10\\client.json"), JSON.toJSONString(clientdata, true).getBytes("utf-8"));

		FileUtils.writeByteArrayToFile(new File("C:\\Users\\雨都停了。\\Desktop\\分布式赛道\\data\\input10\\server.json"), JSON.toJSONString(serverdata, true).getBytes("utf-8"));

		FileUtils.writeByteArrayToFile(new File("C:\\Users\\雨都停了。\\Desktop\\分布式赛道\\data\\input10\\test.json"), JSON.toJSONString(testData, true).getBytes("utf-8"));
	}

	public static JSONObject generateServerConfData(JSONObject inputJSON) {

		JSONObject serverJSON = new JSONObject();

		JSONObject mainConfigJSON = new JSONObject();

		serverJSON.put("channelConfig", inputJSON.getJSONObject("channelConfig").clone());
		serverJSON.put("messageConfig", inputJSON.getJSONObject("messageConfig").clone());
		serverJSON.put("mainConfig", mainConfigJSON);

		mainConfigJSON.put("nodeCount", inputJSON.getJSONObject("mainConfig").getInteger("nodeCount"));
		mainConfigJSON.put("timeOut", inputJSON.getJSONObject("mainConfig").getFloat("timeOut"));
		mainConfigJSON.put("masterIp", inputJSON.getJSONObject("mainConfig").getString("masterIp"));
		mainConfigJSON.put("masterPort", inputJSON.getJSONObject("mainConfig").getInteger("masterPort"));
		mainConfigJSON.put("testData", inputJSON.getJSONObject("mainConfig").getString("testData"));

		int baseCount = inputJSON.getJSONObject("nodeConfig").getIntValue("baseCount");
		int halfFloating = inputJSON.getJSONObject("nodeConfig").getIntValue("halfFloating");

		int high = baseCount + halfFloating;
		int low = baseCount - halfFloating;

		JSONArray maxChannelCount = new JSONArray();
		for (int i = 0; i < mainConfigJSON.getIntValue("nodeCount"); i++) {
			int rand = RandomUtils.nextInt(low, high + 1);
			maxChannelCount.add(rand);
		}
		mainConfigJSON.put("maxChannelCount", maxChannelCount);

		serverJSON.put("index", inputJSON.getInteger("index"));
		serverJSON.put("maxChannelConn", inputJSON.getInteger("maxChannelConn"));

		return serverJSON;
	}

	public static JSONObject generateClientConfData(JSONObject inputJSON) {

		JSONObject clientJSON = new JSONObject();

		JSONObject mainConfigJSON = new JSONObject();

		clientJSON.put("mainConfig", mainConfigJSON);
		clientJSON.put("channelConfig", inputJSON.getJSONObject("channelConfig").clone());
		clientJSON.put("messageConfig", inputJSON.getJSONObject("messageConfig").clone());

		mainConfigJSON.put("nodeCount", inputJSON.getJSONObject("mainConfig").getInteger("nodeCount"));
		mainConfigJSON.put("timeOut", inputJSON.getJSONObject("mainConfig").getFloat("timeOut"));
		mainConfigJSON.put("masterIp", inputJSON.getJSONObject("mainConfig").getString("masterIp"));
		mainConfigJSON.put("masterPort", inputJSON.getJSONObject("mainConfig").getInteger("masterPort"));

		clientJSON.put("index", inputJSON.getInteger("index"));
		clientJSON.put("maxChannelConn", inputJSON.getInteger("maxChannelConn"));

		return clientJSON;
	}

	public static void checkInputJson(JSONObject inputJSON) {

	}

	public static JSONArray generateTestDataJSON(JSONObject inputJSON) throws Exception {

		int queryCount = inputJSON.getJSONObject("queryConfig").getInteger("queryCount");
		double halfFloating = inputJSON.getJSONObject("queryConfig").getDoubleValue("halfFloating");
		double queryRate = inputJSON.getJSONObject("queryConfig").getDoubleValue("queryRate");
		int queryType = inputJSON.getJSONObject("queryConfig").getIntValue("queryType");

		JSONArray delayArray = inputJSON.getJSONObject("queryConfig").getJSONArray("delay");

		double maxdelta = 1.0 / queryRate + halfFloating, mindelta = 1.0 / queryRate - halfFloating;

		double timestamp = 0.0d;

		
		
		JSONArray randomResult = new JSONArray();

		for (int i = 0; i < queryCount; i++) {

			double delta = RandomUtils.nextDouble(mindelta, maxdelta);

			timestamp += delta;

			int[] targetArr = null;

			if (queryType == randomGraph) {
				targetArr = getRandomTarget(inputJSON);
			} else if (queryType == starGraph) {
				targetArr = getStarTarget(inputJSON);
			} else if (queryType == groupGraph) {
				targetArr = getGroupTarget(inputJSON);
			} else if (queryType == bipartiteGraph) {
				targetArr = getBipartiteGraphsTarget(inputJSON);
			} else {
				throw new Exception("不支持的类型");
			}

			double delay = RandomUtils.nextDouble(delayArray.getDoubleValue(0), delayArray.getDoubleValue(1));

			JSONObject prepareJSON = constructPrepareOrSend("prepare", targetArr[0], targetArr[1], delay, timestamp,
					"");
			String sendData = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
			JSONObject sendJSON = constructPrepareOrSend("send", targetArr[0], targetArr[1], 0.0d, timestamp + delay,
					sendData);

			randomResult.add(prepareJSON);
			randomResult.add(sendJSON);

		}

		JSONObject[] randomresultsArr = randomResult.toArray(new JSONObject[randomResult.size()]);
		JSONObject temp = null;

		for (int i = 0; i < randomresultsArr.length - 1; i++) {
			for (int j = randomresultsArr.length - 1; j > i; j--) {

				JSONObject jsonj1 = randomresultsArr[j - 1];
				JSONObject jsonj =randomresultsArr[j];

				if (jsonj1.getDoubleValue("timestamp") > jsonj.getDoubleValue("timestamp")) {
					temp = jsonj1;
					randomresultsArr[j - 1]= jsonj;
					randomresultsArr[j]=temp;
				}
			}

		}

		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll( Arrays.asList(randomresultsArr));
		return jsonArray;
	}

	private static int[] getRandomTarget(JSONObject inputJSON) {
		int nodeCount = inputJSON.getJSONObject("mainConfig").getIntValue("nodeCount");
		int target = RandomUtils.nextInt(1, nodeCount + 1);
		int targetId = RandomUtils.nextInt(1, nodeCount + 1);

		if (target == targetId) {
			target = target == 1 ? ++target : --target;
		}

		int[] targetArr = new int[2];

		targetArr[0] = target;
		targetArr[1] = targetId;

		return targetArr;
	}

	// 星型图
	private static int[] getStarTarget(JSONObject inputJSON) throws Exception {
		int groupCount = inputJSON.getJSONObject("queryConfig").getJSONObject("starConfig").getIntValue("groupCount");
		int nodeCount = inputJSON.getJSONObject("mainConfig").getIntValue("nodeCount");
		double prob = inputJSON.getJSONObject("queryConfig").getJSONObject("starConfig").getDoubleValue("prob");

		int probInt = new Double(prob * 100).intValue();

		int countPerGroup = nodeCount / groupCount;

		if (countPerGroup == 0 || nodeCount <=1) {
			throw new Exception("分组数不能大于节点数或者节点数不能小于2");
		}
		
		if(groupCount <1) {
			throw new Exception("分组数不能小于1");
		}

		
		List<Integer> randomNodesList = getRandomNodes(nodeCount);
		
		int nodeIndex = 1;
		Map<Integer, JSONArray> groupJson = new HashMap<Integer, JSONArray>();
		for (int i = 1; i <= groupCount; i++) {
			JSONArray array = new JSONArray();
			if (i != groupCount) {
				for (int j = nodeIndex; j < (nodeIndex + countPerGroup); j++) {
					array.add(randomNodesList.get(j - 1));
				}
				nodeIndex = nodeIndex + countPerGroup;
			} else {
				for (int j = nodeIndex; j <= nodeCount; j++) {
					array.add(randomNodesList.get(j-1));
				}
			}
			groupJson.put(i, array);
		}
		
		
		
		boolean crossGroup = false;

		Random random = new Random();
		int randInt = random.nextInt(100);

		if (randInt <= probInt) {
			crossGroup = true;
		}

		int[] targetArr = new int[2];

		if (crossGroup) {
			int groupAIndex = RandomUtils.nextInt(1, groupCount + 1);
			int groupBIndex = RandomUtils.nextInt(1, groupCount + 1);
			if (groupAIndex == groupBIndex) {
				groupAIndex = groupAIndex == 1 ? ++groupAIndex : --groupAIndex;
			}

			JSONArray groupAAmember = groupJson.get(groupAIndex);
			int groupAmemberIndex = RandomUtils.nextInt(0, groupAAmember.size());

			targetArr[0] = groupAAmember.getIntValue(groupAmemberIndex);
			JSONArray groupBAmember = groupJson.get(groupBIndex);
			int groupBmemberIndex = RandomUtils.nextInt(0, groupBAmember.size());

			targetArr[1] = groupBAmember.getIntValue(groupBmemberIndex);
		} else {
			int groupIndex = RandomUtils.nextInt(1, groupCount + 1);
			JSONArray groupmember = groupJson.get(groupIndex);
			int memberIndex = RandomUtils.nextInt(0, groupmember.size());
			targetArr[0] = groupmember.getIntValue(memberIndex);

			int memberBIndex = RandomUtils.nextInt(0, groupmember.size());
			if (memberBIndex == memberIndex) {
				memberBIndex = memberBIndex == 0 ? ++memberBIndex : --memberBIndex;
			}
			targetArr[1] = groupmember.getIntValue(memberBIndex);
		}

		return targetArr;
	}

	private static int[] getGroupTarget(JSONObject inputJSON) throws Exception {

		int groupCount = inputJSON.getJSONObject("queryConfig").getJSONObject("starConfig").getIntValue("groupCount");
		int nodeCount = inputJSON.getJSONObject("mainConfig").getIntValue("nodeCount");

		int countPerGroup = nodeCount / groupCount;

		if (countPerGroup == 0 || nodeCount <=1) {
			throw new Exception("分组数不能大于节点数或者节点数不能小于2");
		}
		
		if(groupCount <1) {
			throw new Exception("分组数不能小于1");
		}

		List<Integer> randomNodesList = getRandomNodes(nodeCount);
		
		int nodeIndex = 1;
		Map<Integer, JSONArray> groupJson = new HashMap<Integer, JSONArray>();
		for (int i = 1; i <= groupCount; i++) {
			JSONArray array = new JSONArray();
			if (i != groupCount) {
				for (int j = nodeIndex; j < (nodeIndex + countPerGroup); j++) {
					array.add(randomNodesList.get(j-1));
				}
				nodeIndex = nodeIndex + countPerGroup;
			} else {
				for (int j = nodeIndex; j <= nodeCount; j++) {
					array.add(randomNodesList.get(j-1));
				}
			}
			groupJson.put(i, array);
		}

		int[] targetArr = new int[2];

		int groupIndex = RandomUtils.nextInt(1, groupCount + 1);
		JSONArray groupmember = groupJson.get(groupIndex);
		int memberIndex = RandomUtils.nextInt(0, groupmember.size());
		targetArr[0] = groupmember.getIntValue(memberIndex);

		int memberBIndex = RandomUtils.nextInt(0, groupmember.size());
		if (memberBIndex == memberIndex) {
			memberBIndex = memberBIndex == 0 ? ++memberBIndex : --memberBIndex;
		}
		targetArr[1] = groupmember.getIntValue(memberBIndex);

		return targetArr;

	}

	private static int[] getBipartiteGraphsTarget(JSONObject inputJSON) throws Exception {
		int leftCount = inputJSON.getJSONObject("queryConfig").getJSONObject("bioConfig").getIntValue("leftCount");
		int nodeCount = inputJSON.getJSONObject("mainConfig").getIntValue("nodeCount");
		if (leftCount <= 0 || nodeCount <= leftCount) {
			throw new Exception("左节点数量必须大于0 并且不能大于等于节点数");
		}
        List<Integer> randomNodesList = getRandomNodes(nodeCount);

		int leftNodeIndex = RandomUtils.nextInt(0, leftCount);
		int rightNodeIndex = RandomUtils.nextInt(leftCount, nodeCount);

		int[] target = new int[2];
		int idx = RandomUtils.nextInt() & 1;
		target[idx] = randomNodesList.get(leftNodeIndex);
		target[idx ^ 1] = randomNodesList.get(rightNodeIndex);

		return target;

	}

	private static JSONObject constructPrepareOrSend(String callType, int target, int targetId, double delay,
			double timestamp, String data) {

		JSONObject testdata = new JSONObject();
		testdata.put("callType", callType);
		testdata.put("channelId", 0);
		testdata.put("channelType", 0);
		testdata.put("errCode", 0);
		testdata.put("extMessage", new JSONObject());
		testdata.put("state", -1);
		JSONObject sysMesg = new JSONObject();
		if ("prepare".contentEquals(callType)) {
			sysMesg.put("data", "");
		} else {
			sysMesg.put("data", data);
		}
		sysMesg.put("delay", delay);
		sysMesg.put("target", target);
		testdata.put("targetId", targetId);
		testdata.put("timestamp", timestamp);
		testdata.put("sysMessage", sysMesg);

		return testdata;
	}
	
	
	private static List<Integer> getRandomNodes(int nodeCount) {
		if (nodeMapping != null) {
		    return nodeMapping;
        }
		List<Integer> randoms = new ArrayList<Integer>();
		
		for(int i=0;i<nodeCount;i++) {
			int nodeIndex ;
			do {
				nodeIndex = RandomUtils.nextInt(1, nodeCount+1);
			}while(randoms.contains(nodeIndex));
			randoms.add(nodeIndex);	
		}
		nodeMapping = randoms;
		return randoms;
	}

}
