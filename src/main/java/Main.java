import com.fasterxml.jackson.databind.ObjectMapper;
import conn.Channel;
import conn.GeneralChannel;
import json.Message;
import json.config.Config;

import java.io.File;
import java.util.Date;
import java.util.List;

public class Main {

    public static Config config;

    public static double curTime() {
        return new Date().getTime() / 1000.0;
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        //config = objectMapper.readValue(new File("resources/client.json"), Config.class);
        config = objectMapper.readValue(new File("/home/config/client.json"), Config.class);
        Channel channel = new GeneralChannel();
        channel.initConfig(config);
        if(config.mainConfig.nodeCount == 60 || config.mainConfig.nodeCount == 40 ||config.mainConfig.nodeCount == 800 ) {
        	System.out.println("loopjj 1 ");
        	Node node = new Node(channel, config);
            mainloop(channel, node);
        }else {
        	System.out.println("loop 2 ");
        	mainloop_2(channel);
        }
        
    }

    
    public static void mainloop(Channel channel, Node n){
        //Scheduler scheduler = new Scheduler(channel);
    	Node node = n;
        while (true) {
            try {
                List<Message> message = channel.recv();
                for (Message msg : message) {
                    node.recv(msg);
                }
                Thread.sleep(20);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
    
    
    public static void mainloop_2(Channel channel){
        Control control = new Control(channel);
        while (true) {
            try {
                List<Message> message = channel.recv();
                for (Message msg : message) {
                    control.onRecv(msg);
                }
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}
