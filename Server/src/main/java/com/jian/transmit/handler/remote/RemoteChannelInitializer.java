package com.jian.transmit.handler.remote;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class RemoteChannelInitializer extends ChannelInitializer {

    RemoteMessageToMessageCodec remoteMessageToMessageCodec;
    RemoteChannelInBoundHandler remoteChannelInBoundHandler;

    public RemoteChannelInitializer() {
        remoteMessageToMessageCodec = new RemoteMessageToMessageCodec();
        remoteChannelInBoundHandler = new RemoteChannelInBoundHandler();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(remoteMessageToMessageCodec);
        pipeline.addLast(remoteChannelInBoundHandler);
    }
}
