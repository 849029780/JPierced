package com.jian.handler.local;

import com.jian.beans.Client;
import com.jian.beans.transfer.ConnectReqPacks;
import com.jian.beans.transfer.DisConnectReqPacks;
import com.jian.beans.transfer.TransferDataPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@ChannelHandler.Sharable
public class LocalTcpChannelInBoundHandler extends SimpleChannelInboundHandler<ByteBuf> {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        Channel channel = ctx.channel();
        //设置通道不自动读
        channel.config().setAutoRead(Boolean.FALSE);

        InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();
        Integer port = socketAddress.getPort();
        //通过端口号获取该端口是哪个客户端监听的
        Client client = Constants.PORT_MAPPING_CLIENT.get(port);
        if (Objects.isNull(client) || client.isOnline()) {
            //客户端未连接
            channel.close();
            return;
        }

        //生成通道码
        Long thisChannelHash = System.nanoTime();
        Long tarChannelHash = System.nanoTime();

        channel.attr(Constants.THIS_CHANNEL_HASH_KEY).set(thisChannelHash);
        channel.attr(Constants.TAR_CHANNEL_HASH_KEY).set(tarChannelHash);
        //该通道绑定好客户端通道
        Channel remoteChannel = client.getRemoteChannel();
        channel.attr(Constants.REMOTE_CHANNEL_KEY).set(remoteChannel);

        //当前hash对应本地连接通道
        Map<Long, Channel> localChannelMap = remoteChannel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        localChannelMap.put(thisChannelHash, channel);


        ConnectReqPacks connectReqPacks = new ConnectReqPacks();
        //设置远端绑定时是相反绑定
        connectReqPacks.setThisChannelHash(tarChannelHash);
        connectReqPacks.setTarChannelHash(thisChannelHash);

        //从客户信息中获取该端口对应的哪个本地连接
        InetSocketAddress inetSocketAddress = client.getPortMappingAddress().get(port);
        connectReqPacks.setHost(inetSocketAddress.getHostString());
        connectReqPacks.setPort(inetSocketAddress.getPort());

        //给客户端发送客户端本地的连接信息
        remoteChannel.writeAndFlush(connectReqPacks);
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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        Channel channel = channelHandlerContext.channel();
        Long tarChannelHash = channel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
        //获取该通道上绑定的远程通道
        Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_KEY).get();

        ByteBuf buffer = channelHandlerContext.alloc().buffer(byteBuf.readableBytes());
        buffer.writeBytes(byteBuf);
        TransferDataPacks transferDataPacks = new TransferDataPacks();
        transferDataPacks.setTargetChannelHash(tarChannelHash);
        transferDataPacks.setDatas(buffer);

        remoteChannel.writeAndFlush(transferDataPacks);
    }

}
