package com.jian.transmit;

import com.jian.beans.transfer.ConnectAuthRespPacks;
import com.jian.commons.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class Server {

    private ServerBootstrap serverBootstrap;

    private Server() {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(Constants.BOSS_EVENT_LOOP_GROUP, Constants.WORK_EVENT_LOOP_GROUP);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        serverBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        serverBootstrap.channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : KQueue.isAvailable() ? KQueueServerSocketChannel.class : NioServerSocketChannel.class);
    }

    public static Server getInstance() {
        Server server = new Server();
        return server;
    }

    public static Server getInstance(ChannelInitializer channelInitializer) {
        Server server = getInstance();
        server.serverBootstrap.childHandler(channelInitializer);
        return server;
    }

    public static Server getLocalInstance() {
        return getInstance(Constants.LOCAL_CHANNEL_INITIALIZER);
    }

    public static Server getRemoteInstance() {
        return getInstance(Constants.REMOTE_CHANNEL_INITIALIZER);
    }

    public ChannelFuture listen(Integer port) {
        return serverBootstrap.bind(port);
    }

    public static void listenLocal(Set<Integer> ports, ClientInfo clientInfo) {
        if (!clientInfo.isOnline()) {
            log.info("客户端key:{},name:{}，当前不在线，暂不可监听端口！", clientInfo.getKey(), clientInfo.getName());
            return;
        }
        Constants.FIXED_THREAD_POOL.execute(() -> {
            log.info("客户端key:{},name:{}，需要监听端口:{}", clientInfo.getKey(), clientInfo.getName(), ports);
            StringBuffer stringBuffer = new StringBuffer();
            CountDownLatch countDownLatch = new CountDownLatch(ports.size());
            for (Integer port : ports) {
                Server.getLocalInstance().listen(port).addListener(future -> {
                    try {
                        ChannelFuture future1 = (ChannelFuture) future;
                        Channel channel1 = future1.channel();
                        //映射的客户端本地连接信息
                        NetAddress netAddress = clientInfo.getPortMappingAddress().get(port);
                        stringBuffer.append("服务端端口：");
                        stringBuffer.append(port);
                        if (future1.isSuccess()) {
                            stringBuffer.append("监听成功，映射的本地地址为:");
                            log.info("客户端key：{}，name:{}，监听端口:{}成功！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                            clientInfo.getListenPortMap().put(port, channel1);
                            Constants.PORT_MAPPING_CLIENT.put(port, clientInfo);
                        } else {
                            if (future1.cause() instanceof BindException) {
                                stringBuffer.append("监听失败！该端口已被使用，映射的本地地址为：");
                                log.info("客户端key：{}，name:{}，监听端口:{}失败！该端口已被占用！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                            } else {
                                stringBuffer.append("监听失败！映射的本地地址为：");
                                log.info("客户端key：{}，name:{}，监听端口:{}失败！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                            }
                        }
                        stringBuffer.append(netAddress.getHost());
                        stringBuffer.append(":");
                        stringBuffer.append(netAddress.getPort());
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                log.error("客户端信息:{}，监听端口时CountDownLatch错误！", clientInfo, e);
            }
            ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
            connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.SUCCESS);
            connectAuthRespPacks.setKey(clientInfo.getKey());
            String listenInfo = stringBuffer.toString();
            log.info("客户端key:{},name:{}已连接！{}", clientInfo.getKey(), clientInfo.getName(), listenInfo);
            connectAuthRespPacks.setMsg(listenInfo);
            clientInfo.getRemoteChannel().writeAndFlush(connectAuthRespPacks);
        });
    }


    public static ChannelFuture listenRemote() {
        String tport = Constants.CONFIG.getProperty(Constants.TRANSMIT_PORT_PROPERTY, Constants.DEF_TRANSMIT_PORT);
        Integer port = Integer.valueOf(tport);
        ChannelFuture channelFuture = Server.getRemoteInstance().listen(port);
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                ChannelFuture future1 = (ChannelFuture) future;
                Constants.LISTEN_REMOTE_CHANNEL = future1.channel();
                Constants.LISTEN_REMOTE_CHANNEL.closeFuture().addListener(closeFuture -> {
                    if (closeFuture.isSuccess()) {
                        //将传输端口服务置空
                        Constants.LISTEN_REMOTE_CHANNEL = null;
                        log.info("传输服务已停止..端口:{}", port);
                        //将已连接的客户端重置为离线，及清空相关的数据
                        for (Map.Entry<Long, ClientInfo> clientInfoEntry : Constants.CLIENTS.entrySet()) {
                            ClientInfo clientInfo = clientInfoEntry.getValue();
                            clientInfo.setOnline(false);
                            //客户端连接
                            Channel remoteChannel = clientInfo.getRemoteChannel();
                            Optional.ofNullable(remoteChannel).ifPresent(ch -> {
                                //关闭客户端连接
                                ch.close();
                                clientInfo.setRemoteChannel(null);
                            });
                            Map<Integer, Channel> listenPortMap = clientInfo.getListenPortMap();
                            Optional.ofNullable(listenPortMap).ifPresent(map -> {
                                for (Map.Entry<Integer, Channel> listenEntry : map.entrySet()) {
                                    //移除端口对应的客户端
                                    Constants.PORT_MAPPING_CLIENT.remove(listenEntry.getKey());
                                    //关闭该客户端监听的端口
                                    listenEntry.getValue().close();
                                }
                                clientInfo.setListenPortMap(new ConcurrentHashMap<>());
                            });
                        }
                    } else {
                        log.info("传输服务停止失败..端口:{}", port, closeFuture.cause());
                    }
                });
                log.info("启动传输服务成功！端口:{}", port);
            } else {
                log.error("启动传输服务失败！端口:{}", port, future.cause());
            }
        });
        return channelFuture;
    }

}
