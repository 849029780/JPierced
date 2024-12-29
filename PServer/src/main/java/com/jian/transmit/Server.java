package com.jian.transmit;

import com.jian.beans.transfer.MessageReqPacks;
import com.jian.commons.Constants;
import com.jian.utils.ChannelEventUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
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
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        serverBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        //关闭小包组装成大包，如果都是小流量 则可以关闭，以减少延迟，减少延迟后将会多发送tcp包头，会占用一定带宽
        //serverBootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        serverBootstrap.channel(ChannelEventUtils.getSocketChannelClass());
    }

    public static Server getInstance() {
        return new Server();
    }

    public static Server getInstance(ChannelInitializer<Channel> channelInitializer) {
        Server server = getInstance();
        server.serverBootstrap.childHandler(channelInitializer);
        return server;
    }

    public static Server getLocalInstance() {
        Server server = getInstance(Constants.LOCAL_CHANNEL_INITIALIZER);
        server.group(ChannelEventUtils.getEventLoopGroup(Constants.BOSS_THREAD_NUM), ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return server;
    }

    public static Server getLocalHttpsInstance() {
        Server server = getInstance(Constants.LOCAL_HTTPS_CHANNEL_INITIALIZER);
        server.group(ChannelEventUtils.getEventLoopGroup(Constants.BOSS_THREAD_NUM), ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return server;
    }

    public static Server getRemoteInstance() {
        Server server = getInstance(Constants.REMOTE_CHANNEL_INITIALIZER);
        server.group(ChannelEventUtils.getEventLoopGroup(Constants.BOSS_THREAD_NUM), ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return server;
    }

    public static Server getRemoteAckInstance() {
        Server server = getInstance(Constants.REMOTE_ACK_CHANNEL_INITIALIZER);
        server.group(ChannelEventUtils.getEventLoopGroup(Constants.BOSS_THREAD_NUM), ChannelEventUtils.getEventLoopGroup(Constants.ACK_WORK_THREAD_NUM));
        return server;
    }

    public ChannelFuture listen(Integer port) {
        return serverBootstrap.bind(port);
    }


    public <T> Server childOption(ChannelOption<T> childOption, T value) {
        this.serverBootstrap.childOption(childOption, value);
        return this;
    }

    public void group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        this.serverBootstrap.group(parentGroup, childGroup);
    }

    public static void listenLocal(Set<Integer> ports, ClientInfo clientInfo) {
        if (!clientInfo.isOnline()) {
            log.warn("客户端key:{},name:{}，当前不在线，暂不可监听端口！", clientInfo.getKey(), clientInfo.getName());
            return;
        }
        Constants.FIXED_THREAD_POOL.execute(() -> {
            log.info("客户端key:{},name:{}，需要监听端口:{}", clientInfo.getKey(), clientInfo.getName(), ports);
            StringBuffer stringBuffer = new StringBuffer();
            CountDownLatch countDownLatch = new CountDownLatch(ports.size());
            for (Integer port : ports) {
                //映射的客户端本地连接信息
                NetAddress netAddress = clientInfo.getPortMappingAddress().get(port);
                Server server;
                if (netAddress.getProtocol() == NetAddress.Protocol.HTTPS) {
                    server = Server.getLocalHttpsInstance();
                } else {
                    server = Server.getLocalInstance();
                }
                server.listen(port).addListener(future -> {
                    ChannelFuture future1 = (ChannelFuture) future;
                    Channel channel1 = future1.channel();
                    try {
                        //监听成功后暂时将该端口设置为不自动读消息，处理完之后再将其设置为自动读
                        channel1.config().setAutoRead(Boolean.FALSE);
                        stringBuffer.append("服务端端口：");
                        stringBuffer.append(port);
                        if (future1.isSuccess()) {
                            netAddress.setListen(true);
                            stringBuffer.append("监听成功，映射的本地地址为:");
                            log.info("客户端key：{}，name:{}，监听端口:{}成功！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                            clientInfo.getListenPortMap().put(port, channel1);
                            //设置客户端信息
                            channel1.attr(Constants.LOCAL_BIND_CLIENT_KEY).set(clientInfo);
                            channel1.attr(Constants.LOCAL_BIND_CLIENT_NET_LOCAL_KEY).set(netAddress);
                        } else {
                            if (future1.cause() instanceof BindException) {
                                stringBuffer.append("监听失败！该端口已被使用，映射的本地地址为：");
                                log.warn("客户端key：{}，name:{}，监听端口:{}失败！该端口已被占用！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                            } else {
                                stringBuffer.append("监听失败！映射的本地地址为：");
                                log.warn("客户端key：{}，name:{}，监听端口:{}失败！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                            }
                        }
                        stringBuffer.append(netAddress.getHost());
                        stringBuffer.append(":");
                        stringBuffer.append(netAddress.getPort());
                        //设置为自动读
                        channel1.config().setAutoRead(Boolean.TRUE);
                    } catch (Exception e) {
                        log.error("服务端监听端口:{}处理错误！", port, e);
                        channel1.close();
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

            MessageReqPacks messageReqPacks = new MessageReqPacks();
            String listenInfo = stringBuffer.toString();
            log.info("客户端key:{},name:{}，{}", clientInfo.getKey(), clientInfo.getName(), listenInfo);
            messageReqPacks.setMsg(listenInfo);
            clientInfo.getAckChannel().writeAndFlush(messageReqPacks);
        });
    }


    public static ChannelFuture listenRemote() {
        Integer port = Constants.CONFIG.getTransmitPort();
        ChannelFuture channelFuture = Server.getRemoteInstance().listen(port);
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                ChannelFuture future1 = (ChannelFuture) future;
                Constants.LISTEN_REMOTE_CHANNEL = future1.channel();
                Constants.LISTEN_REMOTE_CHANNEL.closeFuture().addListener(closeFuture -> {
                    if (closeFuture.isSuccess()) {
                        //将传输端口服务置空
                        Constants.LISTEN_REMOTE_CHANNEL = null;
                        log.warn("传输服务已停止..端口:{}", port);
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
                                    //关闭该客户端监听的端口
                                    listenEntry.getValue().close();
                                    NetAddress netAddress = clientInfo.getPortMappingAddress().get(listenEntry.getKey());
                                    Optional.ofNullable(netAddress).ifPresent(addr -> addr.setListen(false));
                                }
                                clientInfo.setListenPortMap(new ConcurrentHashMap<>());
                            });
                        }
                    } else {
                        log.warn("传输服务停止失败..端口:{}", port, closeFuture.cause());
                    }
                });
                log.info("启动传输服务成功！端口:{}", port);
            } else {
                log.error("启动传输服务失败！端口:{}", port, future.cause());
            }
        });
        return channelFuture;
    }


    public static void listenRemoteAck() {
        ChannelFuture channelFuture = Server.getRemoteAckInstance().listen(Constants.CONFIG.getAckPort());
        channelFuture.addListener(future -> {
            ChannelFuture channelFuture1 = (ChannelFuture) future;
            if (channelFuture1.isSuccess()) {
                Channel channel = channelFuture1.channel();
                InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();
                int port = socketAddress.getPort();
                Constants.ACK_PORT = port;
                log.info("启动ack服务成功！端口:{}", port);
            } else {
                log.error("启动ack服务失败！", future.cause());
                throw new IllegalStateException();
            }
        });
    }

}
