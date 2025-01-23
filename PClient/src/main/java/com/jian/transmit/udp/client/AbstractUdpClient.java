package com.jian.transmit.udp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.MultiThreadIoEventLoopGroup;

import java.net.InetSocketAddress;

public abstract class AbstractUdpClient {

    public static MultiThreadIoEventLoopGroup EVENT_LOOP_GROUP;

    Bootstrap bootstrap;

    public abstract boolean support();

    public abstract AbstractUdpClient getInstance();

    public AbstractUdpClient init() {
        bootstrap.handler(Constants.UDP_CHANNEL_INITIALIZER);
        return this;
    }

    public Channel bind(int port, int sourcePort, InetSocketAddress senderAddr) {
        ChannelFuture channelFuture = bootstrap.bind(port).syncUninterruptibly();
        if (channelFuture.isSuccess()) {
            Channel channel = channelFuture.channel();
            channel.attr(Constants.SOURCE_PORT).set(sourcePort);
            channel.attr(Constants.SENDER_ADDR).set(senderAddr);
            return channel;
        }
        throw new RuntimeException(channelFuture.cause());
    }


}
