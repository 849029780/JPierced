package com.jian.transmit.tcp.handler.local;

import com.jian.beans.transfer.TcpTransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
@ChannelHandler.Sharable
public class LocalTcpChannelInBoundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        Channel channel = channelHandlerContext.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        //获取该通道上绑定的远程通道
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();

        byteBuf.retain();
        TcpTransferDataPacks tcpTransferDataPacks = new TcpTransferDataPacks();
        tcpTransferDataPacks.setTarChannelHash(tarChannelHash);
        tcpTransferDataPacks.setDatas(byteBuf);
        try {
            if (Objects.nonNull(remoteChannel)) {
                remoteChannel.writeAndFlush(tcpTransferDataPacks);
            } else {
                ReferenceCountUtil.release(byteBuf);
            }
        } catch (Exception e) {
            ReferenceCountUtil.release(byteBuf);
        }
    }

}
