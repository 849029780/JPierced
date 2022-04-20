package com.jian.handler.local;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class LocalChannelInitializer extends ChannelInitializer {

    private LocalChannelInBoundHandler localChannelInboundHandler;

    public LocalChannelInitializer() {
        localChannelInboundHandler = new LocalChannelInBoundHandler();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(localChannelInboundHandler);
    }
}
