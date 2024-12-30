package com.jian.transmit.tcp.handler.remote;

import com.jian.beans.transfer.BaseTransferPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageCodec;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@ChannelHandler.Sharable
public class CustomChannelInitializer extends ChannelInitializer<Channel> {

    MessageToMessageCodec<ByteBuf, BaseTransferPacks> messageToMessageCodec;

    SimpleChannelInboundHandler<BaseTransferPacks> channelInBoundHandler;

    public CustomChannelInitializer(MessageToMessageCodec<ByteBuf, BaseTransferPacks> messageToMessageCodec, SimpleChannelInboundHandler<BaseTransferPacks> channelInBoundHandler) {
        this.messageToMessageCodec = messageToMessageCodec;
        this.channelInBoundHandler = channelInBoundHandler;
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if(Constants.CONFIG.getTransmit().getUseSsl()){
            pipeline.addFirst(Constants.SSL_TRANSMIT_PORT_CONTEXT.newHandler(channel.alloc()));
        }
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(messageToMessageCodec);
        pipeline.addLast(channelInBoundHandler);
    }
}
