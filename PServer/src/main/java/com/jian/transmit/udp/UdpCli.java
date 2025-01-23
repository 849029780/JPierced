package com.jian.transmit.udp;

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

import java.util.List;


/***
 * udp服务
 * @author Jian
 * @date 2024-12-30
 */
@Slf4j
public class UdpCli {

    private final Bootstrap bootstrap;

    public UdpCli() {
        this.bootstrap = new Bootstrap();
    }


    public static UdpCli getInstance() {
        return new UdpCli();
    }


    public static UdpCli getInstance(ChannelInitializer<DatagramChannel> channelInitializer) {
        UdpCli instance = getInstance();
        instance.bootstrap.channel(ChannelEventUtils.getDatagramChannelClass());
        instance.bootstrap.handler(channelInitializer);
        return instance;
    }


    public static UdpCli getLocalInstance() {
        UdpCli instance = getInstance(Constants.LOCAL_UDP_CHANNEL_INITIALIZER);
        instance.bootstrap.group(ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return instance;
    }

    /***
     * 端口监听
     * @param addressList 待监听的端口和地址列表
     */
    public static void listenLocal(List<NetAddress> addressList, ClientInfo clientInfo) {
        if (!clientInfo.isOnline()) {
            log.warn("客户端key:{},name:{}，当前不在线，暂不可监听udp端口！", clientInfo.getKey(), clientInfo.getName());
            return;
        }

        for (NetAddress netAddress : addressList) {
            UdpCli localInstance = getLocalInstance();
            ChannelFuture future = localInstance.bootstrap.bind(netAddress.getPort());
            future.addListener(fute -> {
                if (!fute.isSuccess()) {
                    log.warn("客户端key:{},name:{}，启动udp端口:{}失败！", clientInfo.getKey(), clientInfo.getName(), netAddress.getPort());
                }
                //本地端口监听状态
                netAddress.setListen(true);
                Channel channel = future.channel();
                //该通道绑定好客户端通道
                Channel remoteChannel = clientInfo.getRemoteChannel();
                Channel ackChannel = clientInfo.getAckChannel();
                channel.attr(Constants.REMOTE_CHANNEL_KEY).set(remoteChannel);
                channel.attr(Constants.REMOTE_ACK_CHANNEL_KEY).set(ackChannel);
                //udp监听的端口保存到客户端口映射map中
                clientInfo.getListenPortMap().put(netAddress.getPort(), channel);
                channel.closeFuture().addListener(UdpCli::closeProcess);
            });
        }
    }

    /***
     * 端口停用
     * @param closeFuture
     */
    private static void closeProcess(Future<? super Void> closeFuture) {
        ChannelFuture channelFuture = (ChannelFuture) closeFuture;


    }


}
