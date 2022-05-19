package com.jian.transmit.handler.remote.transfer;

import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class RemoteChannelInitializer extends ChannelInitializer<Channel> {

    RemoteMessageToMessageCodec remoteMessageToMessageCodec;
    RemoteChannelInBoundHandler remoteChannelInBoundHandler;

    public RemoteChannelInitializer() {
        remoteMessageToMessageCodec = new RemoteMessageToMessageCodec();
        remoteChannelInBoundHandler = new RemoteChannelInBoundHandler();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst(Constants.SSL_TRANSMIT_PORT_CONTEXT.newHandler(channel.alloc()));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(remoteMessageToMessageCodec);
        pipeline.addLast(remoteChannelInBoundHandler);
    }
}
