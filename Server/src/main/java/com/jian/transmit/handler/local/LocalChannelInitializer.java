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

    LocalTcpChannelInBoundHandler localTcpChannelInBoundHandler;

    public LocalChannelInitializer() {
        this.localTcpChannelInBoundHandler = new LocalTcpChannelInBoundHandler();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(localTcpChannelInBoundHandler);
    }

}
