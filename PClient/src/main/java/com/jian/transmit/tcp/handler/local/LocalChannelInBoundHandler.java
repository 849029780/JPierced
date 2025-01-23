package com.jian.transmit.tcp.handler.local;

import com.jian.beans.transfer.req.AutoreadReqPacks;
import com.jian.beans.transfer.req.DisConnectReqPacks;
import com.jian.beans.transfer.TcpTransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Objects;
import java.util.Optional;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/03
 */
@Slf4j
@ChannelHandler.Sharable
public class LocalChannelInBoundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        Channel channel = ctx.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        byteBuf.retain();
        TcpTransferDataPacks tcpTransferDataPacks = new TcpTransferDataPacks();
        tcpTransferDataPacks.setTarChannelHash(tarChannelHash);
        tcpTransferDataPacks.setDatas(byteBuf);
        try {
            Constants.REMOTE_TRANSIMIT_CHANNEL.writeAndFlush(tcpTransferDataPacks);
        } catch (Exception e) {
            ReferenceCountUtil.release(byteBuf);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
        disConnectReqPacks.setTarChannelHash(tarChannelHash);
        Optional.ofNullable(Constants.REMOTE_TRANSIMIT_CHANNEL).ifPresent(ch -> ch.writeAndFlush(disConnectReqPacks));
        log.debug("本地连接:{}已关闭，已通知远程通道tarChannelHash:{}关闭连接..", socketAddress, tarChannelHash);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();

        if (Objects.nonNull(Constants.REMOTE_ACK_CHANNEL)) {
            //向ack通道发送对远程端设置是否可读
            Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
            AutoreadReqPacks autoreadReqPacks = new AutoreadReqPacks();
            autoreadReqPacks.setTarChannelHash(tarChannelHash);
            autoreadReqPacks.setAutoRead(channel.isWritable());
            Constants.REMOTE_ACK_CHANNEL.writeAndFlush(autoreadReqPacks);
        }

        //本地写缓冲状态和远程通道自动读状态设置为一致，如果本地写缓冲满了的话则不允许远程通道自动读
        /*Optional.ofNullable(Constants.REMOTE_TRANSIMIT_CHANNEL).ifPresent(ch -> {
            ch.config().setAutoRead(channel.isWritable());
            ch.attr(Constants.LOCK_CHANNEL_ID_KEY).set(channel.id());
        });*/
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        if (cause instanceof SocketException) {
            log.error("本地通道:{},发生错误！{}", socketAddress, cause.getMessage());
        } else {
            log.error("本地通道:{},发生错误！", socketAddress, cause);
        }
        ctx.close();
    }
}
