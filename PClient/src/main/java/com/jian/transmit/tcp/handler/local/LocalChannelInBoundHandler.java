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
public class LocalChannelInBoundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf byteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel channel = ctx.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        if (Objects.isNull(tarChannelHash)) {
            log.warn("本地通道尚未绑定 tarChannelHash，忽略未就绪数据！");
            ReferenceCountUtil.release(byteBuf);
            return;
        }
        Channel remoteChannel = Constants.REMOTE_TRANSIMIT_CHANNEL;
        if (Objects.nonNull(remoteChannel) && remoteChannel.isActive()) {
            TcpTransferDataPacks tcpTransferDataPacks = new TcpTransferDataPacks();
            tcpTransferDataPacks.setTarChannelHash(tarChannelHash);
            tcpTransferDataPacks.setDatas(byteBuf);
            remoteChannel.writeAndFlush(tcpTransferDataPacks);
        } else {
            ReferenceCountUtil.release(byteBuf);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        // 修复内存泄漏：本地通道断开时，立即从 LOCAL_CHANNEL_MAP 中清理自身
        Long thisChannelHash = channel.attr(Constants.THIS_CHANNEL_HASH_KEY).get();
        if (thisChannelHash != null && Constants.LOCAL_CHANNEL_MAP != null) {
            Constants.LOCAL_CHANNEL_MAP.remove(thisChannelHash);
        }
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        if (tarChannelHash != null) {
            DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
            disConnectReqPacks.setTarChannelHash(tarChannelHash);
            if (Constants.REMOTE_TRANSIMIT_CHANNEL != null && Constants.REMOTE_TRANSIMIT_CHANNEL.isActive()) {
                Constants.REMOTE_TRANSIMIT_CHANNEL.writeAndFlush(disConnectReqPacks);
            }
            log.debug("本地连接:{}已关闭，已通知远程通道tarChannelHash:{}关闭连接..", socketAddress, tarChannelHash);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();

        if (Constants.REMOTE_ACK_CHANNEL != null && Constants.REMOTE_ACK_CHANNEL.isActive()) {
            Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
            if (tarChannelHash != null) {
                //向ack通道发送对远程端设置是否可读
                AutoreadReqPacks autoreadReqPacks = new AutoreadReqPacks();
                autoreadReqPacks.setTarChannelHash(tarChannelHash);
                autoreadReqPacks.setAutoRead(channel.isWritable());
                Constants.REMOTE_ACK_CHANNEL.writeAndFlush(autoreadReqPacks);
            }
        }
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
