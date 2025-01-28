package com.jian.transmit.udp.handler;

import com.jian.beans.UdpSenderChannelInfo;
import com.jian.beans.transfer.UdpTransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Objects;


/***
 * udp数据处理
 * @author Jian
 * @date 2025-01-22
 */
@ChannelHandler.Sharable
public class LocalUdpChannelInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel channel = ctx.channel();
        //远程通道
        Channel remoteTransimitChannel = Constants.REMOTE_TRANSIMIT_CHANNEL;

        Integer sourcePort = channel.attr(Constants.SOURCE_PORT).get();
        InetSocketAddress senderAddr = channel.attr(Constants.SENDER_ADDR).get();



        ByteBuf content = msg.content();
        String senderHost = senderAddr.getHostString();
        Integer senderPort = senderAddr.getPort();
        String sender = senderHost + ":" + senderPort;
        UdpSenderChannelInfo udpSenderChannelInfo = Constants.SENDER_CHANNEL.get(sender);
        if(Objects.isNull(udpSenderChannelInfo)){
            ReferenceCountUtil.release(content);
            return;
        }
        udpSenderChannelInfo.setTime(LocalDateTime.now());

        content.retain();
        UdpTransferDataPacks udpTransferDataPacks = new UdpTransferDataPacks();
        udpTransferDataPacks.setSourcePort(sourcePort);
        udpTransferDataPacks.setSenderHost(senderHost);
        udpTransferDataPacks.setSenderPort(senderPort);
        udpTransferDataPacks.setDatas(content);
        remoteTransimitChannel.writeAndFlush(udpTransferDataPacks);
    }


}
