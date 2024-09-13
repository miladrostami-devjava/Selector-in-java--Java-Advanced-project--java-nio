package com.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NioProxyServer {

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private String targetHost;
    private int targetPort;

    public NioProxyServer(String proxyHost, int proxyPort, String targetHost, int targetPort) throws IOException {
        // Create a Selector
        selector = Selector.open();

        // Create a ServerSocketChannel for the proxy
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(proxyHost, proxyPort));
        serverChannel.configureBlocking(false);

        // Register the proxy server with the Selector for accepting client connections
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // Destination (target) server address
        this.targetHost = targetHost;
        this.targetPort = targetPort;

        System.out.println("Proxy server is running...");
    }

    public void start() throws IOException {
        while (true) {
            selector.select(); // Wait for events
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()) {
                    // Accept a new connection from a client
                    acceptConnection();
                } else if (key.isReadable()) {
                    // Read data from either the client or the target server
                    readFromChannel(key);
                }

                iterator.remove(); // Remove the key after processing
            }
        }
    }

    private void acceptConnection() throws IOException {
        // Accept a new connection from the client
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        // Register the client channel for reading operations
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Client connection accepted: " + clientChannel.getRemoteAddress());
    }

    private void readFromChannel(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(buffer);

        if (bytesRead == -1) {
            // The client or target server has closed the connection
            channel.close();
        } else {
            // Data received from either the client or the target server
            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data);
            System.out.println("Data received: " + new String(data));

            if (key.attachment() == null) {
                // The request is from the client, forward it to the target server
                forwardToTarget(channel, data);
            } else {
                // The response is from the target server, send it back to the client
                SocketChannel clientChannel = (SocketChannel) key.attachment();
                clientChannel.write(ByteBuffer.wrap(data));
                channel.close(); // Close the target server channel after sending the response
            }
        }
    }

    private void forwardToTarget(SocketChannel clientChannel, byte[] requestData) throws IOException {
        // Connect to the target server
        SocketChannel targetChannel = SocketChannel.open(new InetSocketAddress(targetHost, targetPort));
        targetChannel.configureBlocking(false);

        // Send the request to the target server
        targetChannel.write(ByteBuffer.wrap(requestData));

        // Register the target server channel for reading the response
        targetChannel.register(selector, SelectionKey.OP_READ, clientChannel);
        System.out.println("Request sent to the target server: " + targetHost + ":" + targetPort);
    }

    public static void main(String[] args) throws IOException {
        // Proxy server running on port 8080, forwarding requests to the target server on port 80 (e.g., a web server)
        NioProxyServer proxyServer = new NioProxyServer("localhost", 5555, "example.com", 80);
        proxyServer.start();
    }
}
