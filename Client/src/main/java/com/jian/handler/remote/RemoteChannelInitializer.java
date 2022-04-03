package com.jian.handler.remote;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/03
 */
public class RemoteChannelInitializer extends ChannelInitializer {

    private RemoteChannelInBoundHandler remoteChannelInBoundHandler;
    private RemoteMessageToMessageCodec remoteMessageToMessageCodec;

    public RemoteChannelInitializer() {
        remoteChannelInBoundHandler = new RemoteChannelInBoundHandler();
        remoteMessageToMessageCodec = new RemoteMessageToMessageCodec();
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(this.remoteMessageToMessageCodec);
        //pipeline.addLast(new IdleStateHandler(0, 30, Constants.DISCONNECT_HEALTH_SECONDS, TimeUnit.SECONDS));
        pipeline.addLast(this.remoteChannelInBoundHandler);
    }
}
