package com.jian.handler.local;

import com.jian.beans.transfer.DisConnectReqPacks;
import com.jian.beans.transfer.TransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/03
 */
@ChannelHandler.Sharable
public class LocalChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        Channel channel = ctx.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        int readableBytes = byteBuf.readableBytes();
        ByteBuf buffer = ctx.alloc().buffer(readableBytes);
        buffer.writeBytes(byteBuf);

        TransferDataPacks transferDataPacks = new TransferDataPacks();
        transferDataPacks.setTargetChannelHash(tarChannelHash);
        transferDataPacks.setDatas(buffer);

        Constants.REMOTE_CHANNEL.writeAndFlush(transferDataPacks);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
        disConnectReqPacks.setTarChannelHash(tarChannelHash);
        Constants.REMOTE_CHANNEL.writeAndFlush(disConnectReqPacks);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        //本地写缓冲状态和远程通道自动读状态设置为一致，如果本地写缓冲满了的话则不允许远程通道自动读
        Constants.REMOTE_CHANNEL.config().setAutoRead(ctx.channel().isWritable());
    }
}
