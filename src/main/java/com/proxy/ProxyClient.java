package com.proxy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class ProxyClient {


        public static void main(String[] args) throws IOException {
            // Connect to the proxy server
            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 5555));
            clientChannel.configureBlocking(true);

            // Send an HTTP request to the proxy
            String request = "GET / HTTP/1.1\r\nHost: example.com\r\n\r\n";
            ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
            clientChannel.write(buffer);

            // Receive the response from the proxy server
            ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(responseBuffer);

            if (bytesRead != -1) {
                responseBuffer.flip();
                byte[] data = new byte[responseBuffer.limit()];
                responseBuffer.get(data);
                System.out.println("Response received from the target server:\n" + new String(data));
            }

            // Close the connection
            clientChannel.close();
        }
    }
