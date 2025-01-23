package com.jian.transmit.udp.handler;

import com.jian.beans.transfer.UdpTransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;


/***
 * udp数据处理
 * @author Jian
 * @date 2025-01-22
 */
@ChannelHandler.Sharable
public class LocalChannelInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel channel = ctx.channel();
        InetSocketAddress localAddress = (InetSocketAddress) channel.localAddress();
        int localPort = localAddress.getPort();
        InetSocketAddress sender = msg.sender();


        String ip = sender.getHostString();
        int port = sender.getPort();

        //远程通道
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();

        ByteBuf content = msg.content();
        content.retain();

        UdpTransferDataPacks udpTransferDataPacks = new UdpTransferDataPacks();
        udpTransferDataPacks.setSourcePort(localPort);
        udpTransferDataPacks.setSenderHost(ip);
        udpTransferDataPacks.setSenderPort(port);
        udpTransferDataPacks.setDatas(content);
        remoteChannel.writeAndFlush(udpTransferDataPacks);
    }


}
