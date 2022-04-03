package com.jian.start;

import com.jian.commons.Constants;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class App {


    public static void main(String[] args) {
        long key = 123456;
        ClientConnectInfo clientConnectInfo = new ClientConnectInfo();
        clientConnectInfo.setKey(key);
        clientConnectInfo.setName("哈哈哈");
        clientConnectInfo.setPwd("xxxx");

        ConcurrentHashMap<Integer, InetSocketAddress> portMappingAddress = new ConcurrentHashMap<>();
        portMappingAddress.put(3390, new InetSocketAddress("localhost", 3389));
        portMappingAddress.put(3391, new InetSocketAddress("localhost", 3389));
        clientConnectInfo.setPortMappingAddress(portMappingAddress);

        Constants.CLIENTS.put(key, clientConnectInfo);
        startProtocolServer();
    }

    public static void startServer() {
        Server.getInstance(Constants.LOCAL_CHANNEL_INITIALIZER).listen(5555).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("启动对外服务成功..");
            } else {
                System.err.println("启动对外服务失败..");
            }
        });
    }


    public static void startProtocolServer() {
        Server.getRemoteInstance().listen(6666).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("启动传输服务成功..");
            } else {
                System.err.println("启动传输服务失败..");
            }
        });
    }


}
