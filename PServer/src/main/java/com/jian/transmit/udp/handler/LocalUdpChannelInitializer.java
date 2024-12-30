package com.jian.transmit.udp.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;

@ChannelHandler.Sharable
public class LocalUdpChannelInitializer extends ChannelInitializer<DatagramChannel> {


    private final LocalChannelInboundHandler localChannelInboundHandler;

    public LocalUdpChannelInitializer(LocalChannelInboundHandler localChannelInboundHandler) {
        this.localChannelInboundHandler = localChannelInboundHandler;
    }

    @Override
    protected void initChannel(DatagramChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(localChannelInboundHandler);
    }

}
