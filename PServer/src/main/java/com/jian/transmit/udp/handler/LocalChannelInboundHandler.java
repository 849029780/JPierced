package com.jian.transmit.udp.handler;

import com.jian.beans.transfer.UdpTransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;

/***
 * udp数据处理（避免 SimpleChannelInboundHandler 的类型匹配及反射开销）
 * @author Jian
 * @date 2025-01-22
 */
@ChannelHandler.Sharable
public class LocalChannelInboundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DatagramPacket packet) {
            try {
                Channel channel = ctx.channel();
                InetSocketAddress localAddress = (InetSocketAddress) channel.localAddress();
                int localPort = localAddress.getPort();
                InetSocketAddress sender = packet.sender();

                String ip = sender.getHostString();
                int port = sender.getPort();

                //远程通道
                Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();

                ByteBuf content = packet.content();
                if (remoteChannel != null && remoteChannel.isActive()) {
                    content.retain();
                    UdpTransferDataPacks udpTransferDataPacks = new UdpTransferDataPacks();
                    udpTransferDataPacks.setSourcePort(localPort);
                    udpTransferDataPacks.setSenderHost(ip);
                    udpTransferDataPacks.setSenderPort(port);
                    udpTransferDataPacks.setDatas(content);
                    remoteChannel.writeAndFlush(udpTransferDataPacks);
                }
            } finally {
                ReferenceCountUtil.release(packet);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
