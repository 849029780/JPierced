package com.jian.transmit.handler.remote.ack;

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
public class AckChannelInitializer extends ChannelInitializer<Channel> {

    AckMessageToMessageCodec ackMessageToMessageCodec;
    AckChannelInBoundHandler ackChannelInBoundHandler;

    public AckChannelInitializer() {
        ackMessageToMessageCodec = new AckMessageToMessageCodec();
        ackChannelInBoundHandler = new AckChannelInBoundHandler();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst(Constants.SSL_TRANSMIT_PORT_CONTEXT.newHandler(channel.alloc()));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(ackMessageToMessageCodec);
        pipeline.addLast(ackChannelInBoundHandler);
    }
}
