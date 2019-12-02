package conn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class SocketClient {
    private SocketChannel channel;
    private ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 10);
    private StringBuffer stringBuffer = new StringBuffer();
    private ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024 * 10);
    private int left = 0;
    //private Socket socket;

    public SocketClient(InetAddress ip, int port) throws IOException, InterruptedException {
        channel = SocketChannel.open(new InetSocketAddress(ip, port));
        channel.configureBlocking(false);
        while (!channel.isConnected()){
            Thread.sleep(10);
        }
    }

    public void println(String message) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(message.getBytes());
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            channel.write(writeBuffer);
        }
    }

    public String readLine() throws IOException{
        if (left <= 0) {
            readBuffer.clear();
            left = channel.read(readBuffer);
            readBuffer.flip();
        }
        if (left <= 0) {
            //System.out.println("read nothing");
            return "";
        }
        while (left > 0) {
            char cur = (char) readBuffer.get();
            --left;
            if (cur == '\n') {
                String result = stringBuffer.toString();
                stringBuffer.setLength(0);
                return result;
            } else {
                stringBuffer.append(cur);
            }
        }
        System.out.println("no \\n, cur:" + stringBuffer.toString());
        return "";
    }

    public void close() throws IOException {
        if (channel != null && channel.isOpen())
            channel.close();
    }
}