import java.io.*;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;

public class Score {

	static int[] msgcount = new int[] {500, 70 , 650, 100, 1000, 900, 810, 650, 550,750, 100, 1000, 900, 770, 730, 120};
	static double[] timeout = new double[] {5.5, 2.1,2.9, 5.9, 4.2, 3.2 , 2.9, 2.4, 2.9, 2.9, 5.9, 4.2, 3.2, 2.9, 2.9, 5.9 };
	static String pre = "C:\\Users\\雨都停了。\\Downloads\\11-16\\mydata\\d6c63661-8aec-4ae1-921e-9663f5564ddb" + "\\";
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		for(int i =1; i<=16; i++) {
			//readFileByLines(i);
			String path= pre + i + "\\log.csv";
			String path2= pre + i + "\\log1.csv";
			readFileByLines(path, path2);
		}
		
		for(int i =1; i<=16; i++) {
			readFileByLines(i);
		}
	}
	
	public static void readFileByLines(int k) {  
		String path= pre + k + "\\log1.csv";
        File file = new File(path);  
        BufferedReader reader = null;  
        try {  
            //System.out.println("以行为单位读取文件内容，一次读一整行：");  
            reader = new BufferedReader(new FileReader(file));  
            String tempString = null;  
            int line = 0;  
            double totleTime = 0;
            // 一次读入一行，直到读入null为文件结束  
            while ((tempString = reader.readLine()) != null) { 
            	String[] tmp = tempString.split(",");
            	if(Double.valueOf(tmp[2]) > timeout[k-1])
            		continue;
            	line++;
            	totleTime += Double.valueOf(tmp[2]);
                // 显示行号  
                //System.out.println("line " + line + ": " + tempString);  
            }  
            System.out.println("第" + k + "组的通过率 ：" +  new Formatter().format("%.2f", (line*1.0)/msgcount[k-1] ).toString()+ "  平均 延迟   " + new Formatter().format("%.2f", totleTime/msgcount[k-1]).toString() );
            reader.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (reader != null) {  
                try {  
                    reader.close();  
                } catch (IOException e1) {  
                }  
            }  
        }  
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
                //System.out.println("line " + line + ": " + tempString);  
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
