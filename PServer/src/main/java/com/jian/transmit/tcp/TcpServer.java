package com.jian.transmit.tcp;

import com.jian.beans.transfer.req.DisConnectReqPacks;
import com.jian.beans.transfer.req.MessageReqPacks;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.utils.ChannelEventUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
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
public class TcpServer {

    private final ServerBootstrap serverBootstrap;

    private TcpServer() {
        serverBootstrap = new ServerBootstrap();
        // 父通道配置（监听 Socket）：全连接队列调优为 1024，开启端口复用
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        // 子通道配置（传输 Socket）：长连接、端口复用、禁用 Nagle、高低水位调优
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(128 * 1024, 512 * 1024));
        serverBootstrap.channel(ChannelEventUtils.getSocketChannelClass());
    }

    public static TcpServer getInstance() {
        return new TcpServer();
    }

    public static TcpServer getInstance(ChannelInitializer<Channel> channelInitializer) {
        TcpServer tcpServer = getInstance();
        tcpServer.serverBootstrap.childHandler(channelInitializer);
        return tcpServer;
    }

    public static TcpServer getLocalInstance() {
        TcpServer tcpServer = getInstance(Constants.LOCAL_CHANNEL_INITIALIZER);
        tcpServer.group(ChannelEventUtils.getBossLoopGroup(), ChannelEventUtils.getWorkLoopGroup());
        return tcpServer;
    }

    public static TcpServer getLocalHttpsInstance() {
        TcpServer tcpServer = getInstance(Constants.LOCAL_HTTPS_CHANNEL_INITIALIZER);
        tcpServer.group(ChannelEventUtils.getBossLoopGroup(), ChannelEventUtils.getWorkLoopGroup());
        return tcpServer;
    }

    public static TcpServer getRemoteInstance() {
        TcpServer tcpServer = getInstance(Constants.REMOTE_CHANNEL_INITIALIZER);
        tcpServer.group(ChannelEventUtils.getBossLoopGroup(), ChannelEventUtils.getWorkLoopGroup());
        return tcpServer;
    }

    public static TcpServer getRemoteAckInstance() {
        TcpServer tcpServer = getInstance(Constants.REMOTE_ACK_CHANNEL_INITIALIZER);
        tcpServer.group(ChannelEventUtils.getBossLoopGroup(), ChannelEventUtils.getWorkLoopGroup());
        return tcpServer;
    }

    public ChannelFuture listen(Integer port) {
        return serverBootstrap.bind(port);
    }


    public <T> TcpServer childOption(ChannelOption<T> childOption, T value) {
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
        Constants.VIRTUAL_THREAD_POOL.execute(() -> {
            log.info("客户端key:{},name:{}，需要监听端口:{}", clientInfo.getKey(), clientInfo.getName(), ports);
            StringBuffer stringBuffer = new StringBuffer();
            CountDownLatch countDownLatch = new CountDownLatch(ports.size());
            for (Integer port : ports) {
                //映射的客户端本地连接信息
                NetAddress netAddress = clientInfo.getPortMappingAddress().get(port);
                TcpServer tcpServer;
                if (netAddress.getProtocol() == NetAddress.Protocol.HTTPS) {
                    tcpServer = TcpServer.getLocalHttpsInstance();
                } else {
                    tcpServer = TcpServer.getLocalInstance();
                }
                tcpServer.listen(port).addListener(future -> {
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
                            channel1.attr(Constants.CLIENT_INFO_KEY).set(clientInfo);
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
        Integer port = Constants.CONFIG.getTransmit().getPort();
        ChannelFuture channelFuture = TcpServer.getRemoteInstance().listen(port);
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                log.error("启动传输服务失败！端口:{}", port, future.cause());
                return;
            }
            ChannelFuture future1 = (ChannelFuture) future;
            Constants.LISTEN_REMOTE_CHANNEL = future1.channel();
            Constants.LISTEN_REMOTE_CHANNEL.closeFuture().addListener(closeFuture -> {
                if (!closeFuture.isSuccess()) {
                    log.warn("传输服务停止失败..端口:{}", port, closeFuture.cause());
                    return;
                }
                //将传输端口服务置空
                Constants.LISTEN_REMOTE_CHANNEL = null;
                log.warn("传输服务已停止..端口:{}", port);

                //将已连接的客户端重置为离线，及清空相关的数据
                for (Map.Entry<Long, ClientInfo> clientInfoEntry : Constants.CLIENTS.entrySet()) {
                    ClientInfo clientInfo = clientInfoEntry.getValue();
                    clientInfo.setOnline(false);
                    //客户端连接
                    Channel remoteChannel = clientInfo.getRemoteChannel();
                    if (remoteChannel != null) {
                        remoteChannel.close();
                        clientInfo.setRemoteChannel(null);
                    }
                    Map<Integer, Channel> listenPortMap = clientInfo.getListenPortMap();
                    if (listenPortMap != null) {
                        for (Map.Entry<Integer, Channel> listenEntry : listenPortMap.entrySet()) {
                            //关闭该客户端监听的端口
                            listenEntry.getValue().close();
                            NetAddress netAddress = clientInfo.getPortMappingAddress().get(listenEntry.getKey());
                            if (netAddress != null) {
                                netAddress.setListen(false);
                            }
                        }
                        clientInfo.setListenPortMap(new ConcurrentHashMap<>());
                    }
                }
            });
            log.info("启动传输服务成功！端口:{}", port);
        });
        return channelFuture;
    }


    public static void listenRemoteAck() {
        ChannelFuture channelFuture = TcpServer.getRemoteAckInstance().listen(Constants.CONFIG.getTransmit().getAckPort());
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


    /***
     * 关闭本地端口 同时关闭本地端口上的连接
     * @param clientInfo 客户端信息
     * @param serverPort 服务端端口
     */
    public static ChannelFuture closeLocalPort(ClientInfo clientInfo, Channel serverPortChannel, Integer serverPort) {
        //获取该客户端的远程连接，如果连接为空，则该客户端不在线
        Channel remoteChannel = clientInfo.getRemoteChannel();
        if (remoteChannel != null) {
            //客户端在线，则获取该客户端上绑定的本地连接通道
            ConcurrentHashMap<Long, Channel> connectedMap = clientInfo.getConnectedMap();
            if (!connectedMap.isEmpty()) {
                //本地所有连接通道判断端口号是否和当前移除的端口号一致，一致则需要通知远程关闭该通道对应的连接
                for (Map.Entry<Long, Channel> longChannelEntry : connectedMap.entrySet()) {
                    Channel localChannel = longChannelEntry.getValue();
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) localChannel.localAddress();
                    if (inetSocketAddress.getPort() == serverPort) {
                        Long tarChannelHash = localChannel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
                        if (Objects.nonNull(tarChannelHash)) {
                            //通知客户端关闭该端口上的连接通道对应的远程通道
                            DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                            disConnectReqPacks.setTarChannelHash(tarChannelHash);
                            remoteChannel.writeAndFlush(disConnectReqPacks);
                        }
                    }
                }
            }
        }

        //如果不为空，则将channel关闭
        ChannelFuture close = null;
        if (Objects.nonNull(serverPortChannel)) {
            close = serverPortChannel.close();
            close.addListener(future -> {
                if (future.isSuccess()) {
                    log.info("管理员操作关闭端口:{}完成！", serverPort);
                    NetAddress netAddress = clientInfo.getPortMappingAddress().get(serverPort);
                    if (Objects.nonNull(netAddress)) {
                        netAddress.setListen(false);
                    }
                    Channel ackChannel = clientInfo.getAckChannel();
                    if (Objects.nonNull(ackChannel)) {
                        MessageReqPacks messageReqPacks = new MessageReqPacks();
                        messageReqPacks.setMsg("服务端已关闭端口:" + serverPort + "的监听！");
                        ackChannel.writeAndFlush(messageReqPacks);
                    }
                } else {
                    log.warn("管理员操作关闭端口:{}失败！", serverPort);
                }
            });
        }
        return close;
    }



}
