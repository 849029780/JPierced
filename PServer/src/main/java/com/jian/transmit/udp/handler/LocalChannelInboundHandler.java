package com.jian.transmit.udp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class LocalChannelInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = msg.sender();

        ByteBuf content = msg.content();


    }


}
