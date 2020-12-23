package com.company;

import java.io.IOException;
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


public class Server {

    public static void main(String[] args) throws Exception {
        InetAddress hostIPAddress = InetAddress.getByName("localhost");
        int port = 19000;
        Selector selector = Selector.open();
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        ssChannel.socket().bind(new InetSocketAddress(hostIPAddress, port));
        ssChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        while (true) {
            if (selector.select() <= 0) continue;
            processReadySet(selector.selectedKeys());
        }
    }

    private static void processReadySet(Set<SelectionKey> readySet) throws Exception {
        SelectionKey key = null;
        Iterator iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
            if (key.isAcceptable()) {
                processAccept(key);
            }
            if (key.isReadable()){
                String msg = processRead(key);
                if (msg.length() > 0) echoMsg(key, msg);
            }
        }
    }

    private static void echoMsg(SelectionKey key, String msg) throws IOException {
        SocketChannel sChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        sChannel.write(buffer);
    }

    private static String processRead(SelectionKey key) throws Exception {
        SocketChannel sChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesCount = sChannel.read(buffer);
        String msg = "";
        if (bytesCount > 0) {
            buffer.flip();
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buffer);
            msg = charBuffer.toString();
            System.out.println("Получено сообщение: " + msg);
        }
        return msg;
    } 

    private static void processAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sChannel = ssChannel.accept();
        sChannel.configureBlocking(false);
        sChannel.register(key.selector(), SelectionKey.OP_READ);
    }
}
