package com.jian.transmit.tcp.handler.local;

import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class LocalHttpsChannelInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst(Constants.LOCAL_SSL_CONTEXT.newHandler(channel.alloc()));
        pipeline.addLast(Constants.LOCAL_CHANNEL_IN_BOUND_HANDLER);
    }
}
