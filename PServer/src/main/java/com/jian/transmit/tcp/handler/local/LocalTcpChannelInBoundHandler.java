package com.jian.transmit.tcp.handler.local;

import com.jian.beans.transfer.TcpTransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
public class LocalTcpChannelInBoundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf byteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel channel = ctx.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        if (Objects.isNull(tarChannelHash)) {
            log.warn("服务端本地通道尚未绑定 tarChannelHash，忽略未就绪数据！");
            ReferenceCountUtil.release(byteBuf);
            return;
        }
        //获取该通道上绑定的远程通道
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();

        if (Objects.nonNull(remoteChannel) && remoteChannel.isActive()) {
            TcpTransferDataPacks tcpTransferDataPacks = new TcpTransferDataPacks();
            tcpTransferDataPacks.setTarChannelHash(tarChannelHash);
            tcpTransferDataPacks.setDatas(byteBuf);
            remoteChannel.writeAndFlush(tcpTransferDataPacks);
        } else {
            ReferenceCountUtil.release(byteBuf);
        }
    }

}
