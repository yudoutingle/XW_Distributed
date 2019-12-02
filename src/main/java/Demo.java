import java.io.File;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import conn.Channel;
import conn.GeneralChannel;
import json.Message;
import json.config.Config;

public class Demo implements Runnable {
	
	public  Config config;
	Channel channel = new GeneralChannel();
	public Node node;
	
	public Demo()  throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        config = objectMapper.readValue(new File("resources/client.json"), Config.class);
        //config = objectMapper.readValue(new File("/home/config/client.json"), Config.class);
        channel.initConfig(config);
        node = new Node(channel, config);
        
	}

    public double curTime() {
        return new Date().getTime() / 1000.0;
    }


    @Override
    public void run() {
    	if(config.mainConfig.nodeCount == 60 || config.mainConfig.nodeCount == 40 ||config.mainConfig.nodeCount == 800 ) {
        	System.out.println("loop 1 ");
            mainloop(channel);
        }else {
        	System.out.println("loop 2  ");
        	mainloop_2(channel);
        }
    }
    
    public void mainloop(Channel channel) {
        //Scheduler scheduler = new Scheduler(channel);
    	
        while (true) {
            try {
                List<Message> message = channel.recv();
                for (Message msg : message) {
                    node.recv(msg);
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
