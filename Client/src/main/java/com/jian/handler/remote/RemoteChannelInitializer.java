package com.jian.handler.remote;

import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslHandler;

/**
 * 描述
 * @author Jian
 * @date 2022/04/03
 */
public class RemoteChannelInitializer extends ChannelInitializer {

    private final RemoteChannelInBoundHandler remoteChannelInBoundHandler;
    private final RemoteMessageToMessageCodec remoteMessageToMessageCodec;

    public RemoteChannelInitializer() {
        remoteChannelInBoundHandler = new RemoteChannelInBoundHandler();
        remoteMessageToMessageCodec = new RemoteMessageToMessageCodec();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst(Constants.REMOTE_SSL_CONTEXT.newHandler(channel.alloc()));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(this.remoteMessageToMessageCodec);
        pipeline.addLast(this.remoteChannelInBoundHandler);
    }
}
