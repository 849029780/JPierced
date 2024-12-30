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
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/***
 * udp服务
 * @author Jian
 * @date 2024-12-30
 */
@Slf4j
public class UdpServer {

    private final Bootstrap bootstrap;

    public UdpServer() {
        this.bootstrap = new Bootstrap();
    }


    public static UdpServer getInstance() {
        return new UdpServer();
    }


    public static UdpServer getInstance(ChannelInitializer<DatagramChannel> channelInitializer) {
        UdpServer instance = getInstance();
        instance.bootstrap.handler(channelInitializer);
        return instance;
    }


    public static UdpServer getLocalInstance() {
        UdpServer instance = getInstance(Constants.LOCAL_UDP_CHANNEL_INITIALIZER);
        instance.bootstrap.group(ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return instance;
    }

    /***
     * 端口监听
     * @param addressList 待监听的端口和地址列表
     * @return 监听结果
     */
    public static void listenLocal(List<NetAddress> addressList, ClientInfo clientInfo) {
        if (!clientInfo.isOnline()) {
            log.warn("客户端key:{},name:{}，当前不在线，暂不可监听udp端口！", clientInfo.getKey(), clientInfo.getName());
            return;
        }

        for (NetAddress netAddress : addressList) {
            UdpServer localInstance = getLocalInstance();
            ChannelFuture future = localInstance.bootstrap.bind(netAddress.getPort());
            future.addListener(fute -> {
                if (!fute.isSuccess()) {
                    log.warn("客户端");
                }

                //本地端口启动状态设置
                netAddress.setListen(true);

                ChannelFuture channelFuture = (ChannelFuture) future;
                Channel channel = channelFuture.channel();
                channel.attr()




            });
        }


    }


}
