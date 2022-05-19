package com.jian.transmit.handler.local;

import com.jian.beans.transfer.AutoreadReqPacks;
import com.jian.beans.transfer.ConnectReqPacks;
import com.jian.beans.transfer.DisConnectReqPacks;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/***
 *
 * @author Jian
 * @date 2022/4/6
 */
@Slf4j
@ChannelHandler.Sharable
public class LocalChannelInBoundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        Channel channel = ctx.channel();
        //设置通道不自动读
        channel.config().setAutoRead(Boolean.FALSE);

        InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();

        Integer port = socketAddress.getPort();
        log.debug("端口:{}，新的连接..", port);
        //通过端口号获取该端口是哪个客户端监听的
        ClientInfo clientInfo = channel.parent().attr(Constants.LOCAL_BIND_CLIENT_KEY).get();
        if (Objects.isNull(clientInfo) || !clientInfo.isOnline()) {
            log.info("该端口:{}，未找到在线的客户端，连接已关闭..", port);
            //客户端未连接
            channel.close();
            return;
        }

        //生成通道码
        Long thisChannelHash = System.nanoTime();
        Long tarChannelHash = thisChannelHash >> 2;

        channel.attr(Constants.THIS_CHANNEL_HASH_KEY).set(thisChannelHash);
        channel.attr(Constants.TAR_CHANNEL_HASH_KEY).set(tarChannelHash);
        //该通道绑定好客户端通道
        Channel remoteChannel = clientInfo.getRemoteChannel();
        channel.attr(Constants.REMOTE_CHANNEL_KEY).set(remoteChannel);

        //当前hash对应本地连接通道
        Map<Long, Channel> localChannelMap = remoteChannel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        localChannelMap.put(thisChannelHash, channel);


        ConnectReqPacks connectReqPacks = new ConnectReqPacks();
        //设置远端绑定时是相反绑定
        connectReqPacks.setThisChannelHash(tarChannelHash);
        connectReqPacks.setTarChannelHash(thisChannelHash);

        //从客户信息中获取该端口对应的哪个本地连接
        NetAddress netAddress = channel.parent().attr(Constants.LOCAL_BIND_CLIENT_NET_LOCAL_KEY).get();
        connectReqPacks.setHost(netAddress.getHost());
        connectReqPacks.setPort(netAddress.getPort());
        ChannelPipeline pipeline = channel.pipeline();
        //http协议时需要额外添加handler处理
        switch (netAddress.getProtocol()) {
            case HTTPS -> {
                pipeline.addLast(new LocalHttpByteToMessageCodec(netAddress.getHost(), netAddress.getPort()));

                //是否启用https
                if (Constants.IS_ENABLE_HTTPS) {
                    connectReqPacks.setProtocol(ConnectReqPacks.Protocol.HTTPS);
                } else {
                    log.warn("https未启用，暂不可使用https协议访问！已切换为http访问！");
                    connectReqPacks.setProtocol(ConnectReqPacks.Protocol.HTTP);
                }
            }
            case HTTP -> {
                pipeline.addLast(new LocalHttpByteToMessageCodec(netAddress.getHost(), netAddress.getPort()));
                connectReqPacks.setProtocol(ConnectReqPacks.Protocol.HTTP);
            }
            case TCP -> connectReqPacks.setProtocol(ConnectReqPacks.Protocol.TCP);
        }
        //tcp处理
        pipeline.addLast(Constants.LOCAL_TCP_CHANNEL_IN_BOUND_HANDLER);
        //给客户端发送客户端本地的连接信息
        Optional.of(remoteChannel).ifPresent(ch -> ch.writeAndFlush(connectReqPacks));
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        //获取目标通道
        Long thisChannelHash = channel.attr(Constants.THIS_CHANNEL_HASH_KEY).get();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();

        //获取该通道上绑定的远程通道
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();
        if (Objects.isNull(remoteChannel)) {
            return;
        }
        //从远程通道上获取绑定的本地通道信息，且移除本地通道
        Map<Long, Channel> localChannelMap = remoteChannel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        localChannelMap.remove(thisChannelHash);

        DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
        disConnectReqPacks.setTarChannelHash(tarChannelHash);
        if (remoteChannel.isOpen()) {
            //发送关闭通道消息
            remoteChannel.writeAndFlush(disConnectReqPacks);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        Channel channel = ctx.channel();
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();
        if (Objects.isNull(remoteChannel)) {
            return;
        }
        ChannelId id = channel.id();
        ChannelId channelId = remoteChannel.attr(Constants.LOCK_CHANNEL_ID_KEY).get();
        //------这里一定要注意，如果本地通道被写满的同时并且通道被关闭，一定要将设置为不自动读的远程通道重新设置为可读，否则将会出现数据不流通的严重问题！！！
        //用于通道移除时使用，如果该本地通道移除时发现自己锁定了远程通道，则需要解锁，如果非当前本地通道锁定的远程通道，则不允许被解锁
        if (id.equals(channelId) && !remoteChannel.config().isAutoRead()) {
            remoteChannel.config().setAutoRead(Boolean.TRUE);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        byteBuf.retain();
        channelHandlerContext.fireChannelRead(byteBuf);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();
        //获取该通道上绑定的远程通道
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();
        //ack通道
        Channel ackChannel = remoteChannel.attr(Constants.REMOTE_ACK_CHANNEL_KEY).get();

        Optional.ofNullable(ackChannel).ifPresent(ch -> {
            //向ack通道发送设置是否自动读消息
            Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
            AutoreadReqPacks autoreadReqPacks = new AutoreadReqPacks();
            autoreadReqPacks.setAutoRead(channel.isWritable());
            autoreadReqPacks.setTarChannelHash(tarChannelHash);
            ch.writeAndFlush(autoreadReqPacks);
        });


        //本地写缓冲状态和远程通道自动读状态设置为一致，如果本地写缓冲满了的话则不允许远程通道自动读
        /*Optional.ofNullable(remoteChannel).ifPresent(rch -> {
            rch.config().setAutoRead(channel.isWritable());
            rch.attr(Constants.LOCK_CHANNEL_ID_KEY).set(channel.id());
        });*/
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.localAddress();
        SocketAddress socketAddress1 = channel.remoteAddress();
        if (cause instanceof SocketException cause1) {
            if (cause1.getMessage().equals("Connection reset")) {
                log.error("本地通道连接被重置！本地地址及端口:{}，远程连接：{}", socketAddress, socketAddress1);
                return;
            }
        }
        log.error("本地通道发生错误！本地地址及端口:{}，远程连接：{}", socketAddress, socketAddress1, cause);
        ctx.close();
    }
}
