package com.jian.transmit.handler.local;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class LocalChannelInitializer extends ChannelInitializer {

    LocalChannelInBoundHandler localChannelInBoundHandler;

    public LocalChannelInitializer() {
        this.localChannelInBoundHandler = new LocalChannelInBoundHandler();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(localChannelInBoundHandler);
    }

}
