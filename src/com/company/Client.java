package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

public class Client {
    public static BufferedReader userInputReader = null;

    public static void main(String[] args) throws Exception {
        InetAddress serverIPAddress = InetAddress.getByName("localhost");
        int port = 19000;
        InetSocketAddress serverAddress = new InetSocketAddress(serverIPAddress, port);
        Selector selector = Selector.open();
        
        try (SocketChannel channel = SocketChannel.open()){
            channel.configureBlocking(false);
            channel.connect(serverAddress);
            int operations = SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE;
            channel.register(selector, operations);
            userInputReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                if (selector.select() <= 0) continue;
                boolean doneStatus = processReadySet(selector.selectedKeys());
                if (doneStatus) break;
            }
        }
        
       
    }

    private static boolean processReadySet(Set<SelectionKey> readySet) throws Exception {
        SelectionKey key = null;
        Iterator iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
            if (key.isConnectable()){
                boolean connected = processConnect(key);
                if (!connected) return true;
            }
            if (key.isReadable()) {
                String msg = processRead(key);
                System.out.println("[Серер:] " + msg);
            }
            if (key.isWritable()) {
                String msg = getUserInput();
                if (msg.equalsIgnoreCase("пока"))
                    return true;
                processWrite(key, msg);
            }            
        }
        return false;
    }

    private static void processWrite(SelectionKey key, String msg) throws IOException {
        SocketChannel sChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        sChannel.write(buffer);
    }

    private static String getUserInput() throws IOException {
        String prompt = "Наберите сообщение (или Пока для выхода): ";
        System.out.print(prompt);
        String userMsg = userInputReader.readLine();
        return userMsg;
    }

    private static String processRead(SelectionKey key) throws Exception {
        SocketChannel sChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        sChannel.read(buffer);
        buffer.flip();
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = decoder.decode(buffer);
        String msg = charBuffer.toString();
        return msg;
    }

    private static boolean processConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel)key.channel();
        try {
            while (channel.isConnectionPending())
                channel.finishConnect();
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
