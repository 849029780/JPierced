package com.jian.transmit.udp;

import com.jian.beans.transfer.req.MessageReqPacks;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.utils.ChannelEventUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.util.List;
import java.util.Set;


/***
 * udp服务
 * @author Jian
 * @date 2024-12-30
 */
@Slf4j
public class UdpClient {

    private final Bootstrap bootstrap;

    public UdpClient() {
        this.bootstrap = new Bootstrap();
    }


    public static UdpClient getInstance() {
        return new UdpClient();
    }


    public static UdpClient getInstance(ChannelInitializer<DatagramChannel> channelInitializer) {
        UdpClient instance = getInstance();
        instance.bootstrap.channel(ChannelEventUtils.getDatagramChannelClass());
        instance.bootstrap.handler(channelInitializer);
        return instance;
    }


    public static UdpClient getLocalInstance() {
        UdpClient instance = getInstance(Constants.LOCAL_UDP_CHANNEL_INITIALIZER);
        instance.bootstrap.group(ChannelEventUtils.getWorkLoopGroup());
        return instance;
    }

    /***
     * 端口监听
     * @param ports 待监听的端口列表
     */
    public static void listenLocal(Set<Integer> ports, ClientInfo clientInfo) {
        if (!clientInfo.isOnline()) {
            log.warn("客户端key:{},name:{}，当前不在线，暂不可监听udp端口！", clientInfo.getKey(), clientInfo.getName());
            return;
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (Integer port : ports) {
            stringBuffer.append("服务端udp端口：");
            stringBuffer.append(port);
            UdpClient localInstance = getLocalInstance();
            ChannelFuture future = localInstance.bootstrap.bind(port);
            NetAddress netAddress = clientInfo.getPortMappingAddress().get(port);
            future.addListener(fute -> {
                if (!fute.isSuccess()) {
                    log.warn("客户端key:{},name:{}，监听udp端口:{}失败！", clientInfo.getKey(), clientInfo.getName(), port);
                    if (fute.cause() instanceof BindException) {
                        stringBuffer.append("监听失败！该端口已被使用，映射的本地地址为：");
                        log.warn("客户端key：{}，name:{}，监听udp端口:{}失败！该端口已被占用！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                    } else {
                        stringBuffer.append("监听失败！映射的本地地址为：");
                        log.warn("客户端key：{}，name:{}，监听udp端口:{}失败！映射的本地地址为:{}", clientInfo.getKey(), clientInfo.getName(), port, netAddress);
                    }
                } else {
                    stringBuffer.append("监听udp端口成功，映射的本地地址为:");
                    //本地端口监听状态
                    netAddress.setListen(true);
                    Channel channel = future.channel();
                    //该通道绑定好客户端通道
                    Channel remoteChannel = clientInfo.getRemoteChannel();
                    Channel ackChannel = clientInfo.getAckChannel();
                    channel.attr(Constants.REMOTE_CHANNEL_KEY).set(remoteChannel);
                    channel.attr(Constants.REMOTE_ACK_CHANNEL_KEY).set(ackChannel);
                    //udp监听的端口保存到客户端口映射map中
                    clientInfo.getListenPortMap().put(port, channel);
                    channel.closeFuture().addListener(UdpClient::closeProcess);
                }
                stringBuffer.append(netAddress.getHost());
                stringBuffer.append(":");
                stringBuffer.append(port);


                MessageReqPacks messageReqPacks = new MessageReqPacks();
                String listenInfo = stringBuffer.toString();
                log.info("客户端key:{},name:{}，{}", clientInfo.getKey(), clientInfo.getName(), listenInfo);
                messageReqPacks.setMsg(listenInfo);
                clientInfo.getAckChannel().writeAndFlush(messageReqPacks);

            });
        }


        MessageReqPacks messageReqPacks = new MessageReqPacks();
        String listenInfo = stringBuffer.toString();
        log.info("客户端key:{},name:{}，{}", clientInfo.getKey(), clientInfo.getName(), listenInfo);
        messageReqPacks.setMsg(listenInfo);
        clientInfo.getAckChannel().writeAndFlush(messageReqPacks);


    }

    /***
     * 端口停用
     * @param closeFuture
     */
    private static void closeProcess(Future<? super Void> closeFuture) {
        ChannelFuture channelFuture = (ChannelFuture) closeFuture;


    }


    public static void closeLocalPort() {

    }
}
