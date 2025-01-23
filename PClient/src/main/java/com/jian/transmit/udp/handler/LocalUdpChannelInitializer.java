package com.jian.transmit.udp.handler;

import com.jian.commons.Constants;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;

@ChannelHandler.Sharable
public class LocalUdpChannelInitializer extends ChannelInitializer<DatagramChannel> {

    @Override
    protected void initChannel(DatagramChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(Constants.LOCAL_UDP_CHANEL_INBOUND_HANDLER);
    }

}
