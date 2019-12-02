import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.*;

public class Quchong {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = "C:\\Users\\雨都停了。\\Desktop\\分布式赛道\\server1107 o2\\server\\cmake-build-release\\attach.log";
		String path2 = "C:\\Users\\雨都停了。\\Desktop\\分布式赛道\\server1107 o2\\server\\cmake-build-release\\attach1.log";
		readFileByLines(path, path2);

	}

	public static void readFileByLines(String path, String path2) {  
        File file = new File(path);  
        BufferedReader reader = null;
        BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(path2),true));
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		};
        Set<String> set = new HashSet<>();
        try {  
            //System.out.println("以行为单位读取文件内容，一次读一整行：");  
            reader = new BufferedReader(new FileReader(file));  
            writer = new BufferedWriter(new FileWriter(new File(path2),true));  
            String tempString = null;  
            // 一次读入一行，直到读入null为文件结束  
            while ((tempString = reader.readLine()) != null) {  
            	String[] tmp = tempString.split(",");
                // 显示行号  
            	if(!set.contains(tmp[0])) {
            		set.add(tmp[0]);
            		writer.write(tempString+"\n");
            	}
            }  
            reader.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (reader != null) {  
                try {  
                    reader.close(); 
                    writer.close();
                } catch (IOException e1) {  
                }  
            }  
        }  
    }  
	

	
}
