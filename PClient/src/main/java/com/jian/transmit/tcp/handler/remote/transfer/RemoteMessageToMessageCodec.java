package com.jian.transmit.tcp.handler.remote.transfer;

import com.jian.beans.transfer.*;
import com.jian.beans.transfer.beans.NetAddr;
import com.jian.beans.transfer.req.*;
import com.jian.beans.transfer.resp.ConnectAuthRespPacks;
import com.jian.beans.transfer.resp.HealthRespPacks;
import com.jian.commons.Constants;
import com.jian.transmit.tcp.client.AbstractTcpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
@ChannelHandler.Sharable
public class RemoteMessageToMessageCodec extends MessageToMessageCodec<ByteBuf, BaseTransferPacks> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks, List<Object> out) {
        int packSize = Constants.BASE_PACK_SIZE;
        byte type = baseTransferPacks.getType();
        ByteBuf buffer = ctx.alloc().buffer();
        switch (type) {
            case 3 -> { //发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(disConnectReqPacks.getTarChannelHash());
                out.add(buffer);
            }
            case 4 -> { //发起认证
                ConnectAuthReqPacks connectAuthReqPacks = (ConnectAuthReqPacks) baseTransferPacks;
                packSize += 8 + 4;
                String pwd = connectAuthReqPacks.getPwd();
                byte[] pwdBytes = pwd.getBytes(StandardCharsets.UTF_8);
                int pwdLen = pwdBytes.length;
                packSize += pwdLen;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectAuthReqPacks.getKey());
                buffer.writeInt(pwdLen);
                if (pwdLen > 0) {
                    buffer.writeBytes(pwdBytes);
                }
                out.add(buffer);
            }
            case 7 -> { //传输数据
                TcpTransferDataPacks tcpTransferDataPacks = (TcpTransferDataPacks) baseTransferPacks;
                packSize += 8;
                ByteBuf datas = tcpTransferDataPacks.getDatas();
                int readableBytes = datas.readableBytes();
                packSize += readableBytes;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(tcpTransferDataPacks.getTarChannelHash());
                out.add(buffer);
                out.add(datas);
            }
            case 8 -> {//心跳请求
                HealthReqPacks healthReqPacks = (HealthReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(healthReqPacks.getMsgId());
                out.add(buffer);
            }
            case 20 -> { //udp数据传输
                UdpTransferDataPacks udpTransferDataPacks = (UdpTransferDataPacks) baseTransferPacks;
                ByteBuf data = udpTransferDataPacks.getDatas();
                int dataLen = data.readableBytes();
                packSize += 4 + 1 + udpTransferDataPacks.getIpLen() + 4 + dataLen;
                buffer.writeInt(packSize);
                buffer.writeInt(udpTransferDataPacks.getSourcePort());

                String senderHost = udpTransferDataPacks.getSenderHost();
                byte[] bytes = senderHost.getBytes(StandardCharsets.UTF_8);

                buffer.writeByte(bytes.length);
                buffer.writeBytes(bytes);
                buffer.writeInt(udpTransferDataPacks.getSenderPort());
                buffer.writeInt(dataLen);
                out.add(buffer);
                out.add(data);
            }
        }

    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        int packSize = byteBuf.readInt();
        byte type = byteBuf.readByte();
        switch (type) {
            case 3 -> {//发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                long tarChannelHash = byteBuf.readLong();
                disConnectReqPacks.setTarChannelHash(tarChannelHash);
                list.add(disConnectReqPacks);
            }
            case 5 -> {
                ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
                long key = byteBuf.readLong();
                byte state = byteBuf.readByte();
                int ackPort = byteBuf.readInt();
                int msgLen = byteBuf.readInt();
                if (msgLen > 0) {
                    ByteBuf msgBuff = byteBuf.readSlice(msgLen);
                    String msg = msgBuff.toString(StandardCharsets.UTF_8);
                    connectAuthRespPacks.setMsg(msg);
                }
                connectAuthRespPacks.setKey(key);
                connectAuthRespPacks.setState(state);
                connectAuthRespPacks.setAckPort(ackPort);
                connectAuthRespPacks.setMsgLen(msgLen);
                connectAuthRespPacks.setPackSize(packSize);
                list.add(connectAuthRespPacks);
            }
            case 7 -> { //传输数据
                TcpTransferDataPacks tcpTransferDataPacks = new TcpTransferDataPacks();
                long targetChannelHash = byteBuf.readLong();
                int readableBytes = byteBuf.readableBytes();
                ByteBuf buffer = byteBuf.readRetainedSlice(readableBytes);
                tcpTransferDataPacks.setTarChannelHash(targetChannelHash);
                tcpTransferDataPacks.setDatas(buffer);
                list.add(tcpTransferDataPacks);
            }
            case 9 -> {//心跳响应
                HealthRespPacks healthRespPacks = new HealthRespPacks();
                long msgId = byteBuf.readLong();
                healthRespPacks.setMsgId(msgId);
                list.add(healthRespPacks);
            }
            case 10 -> {//断开客户端
                DisConnectClientReqPacks disConnectClientReqPacks = new DisConnectClientReqPacks();
                byte code = byteBuf.readByte();
                disConnectClientReqPacks.setPackSize(packSize);
                disConnectClientReqPacks.setCode(code);
                list.add(disConnectClientReqPacks);
            }
            case 20 -> { //udp数据传输
                UdpTransferDataPacks udpTransferDataPacks = new UdpTransferDataPacks();
                int sourcePort = byteBuf.readInt();
                byte ipLen = byteBuf.readByte();
                CharSequence charSequence = byteBuf.readCharSequence(ipLen, StandardCharsets.UTF_8);
                String ipHost = charSequence.toString();
                int port = byteBuf.readInt();
                int dataLen = byteBuf.readInt();
                ByteBuf data = byteBuf.readRetainedSlice(dataLen);
                udpTransferDataPacks.setSourcePort(sourcePort);
                udpTransferDataPacks.setIpLen(ipLen);
                udpTransferDataPacks.setSenderHost(ipHost);
                udpTransferDataPacks.setSenderPort(port);
                udpTransferDataPacks.setDatas(data);
                list.add(udpTransferDataPacks);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();

        log.info("传输通道断开...");
        //如果关闭的是传输通道，则判断ack通道是否还连接，连接则需关闭
        if (Objects.nonNull(Constants.REMOTE_ACK_CHANNEL)) {
            Constants.REMOTE_ACK_CHANNEL.close();
        }
        //将连接置空
        Constants.REMOTE_TRANSIMIT_CHANNEL = null;

        if (Objects.nonNull(Constants.LOCAL_CHANNEL_MAP)) {
            //和服务端的连接断开，关闭本地所有的连接
            for (Map.Entry<Long, Channel> entry : Constants.LOCAL_CHANNEL_MAP.entrySet()) {
                Optional.ofNullable(entry.getValue()).ifPresent(ch -> ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE));
            }
            //本地连接关闭后置空
            Constants.LOCAL_CHANNEL_MAP = null;
        }
        AbstractTcpClient abstractTcpClient = channel.attr(Constants.CLIENT_KEY).get();
        Optional.ofNullable(abstractTcpClient).ifPresent(cli -> {
            if (abstractTcpClient.isCanReconnect()) {
                //重连
                Constants.CACHED_EXECUTOR_POOL.execute(cli::reConnct);
            } else {
                System.exit(0);
            }
        });

    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();
        boolean writable = channel.isWritable();

        //远程通道写缓冲状态和本地自动读状态设置为一致，如果通道缓冲写满了，则不允许本地通道自动读
        for (Map.Entry<Long, Channel> entry : Constants.LOCAL_CHANNEL_MAP.entrySet()) {
            Channel localChannel = entry.getValue();
            Optional.ofNullable(localChannel).ifPresent(ch -> ch.config().setAutoRead(writable));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        if (cause instanceof SocketException) {
            log.error("远程通道:{},发生错误！{}", socketAddress, cause.getMessage());
        } else {
            log.error("远程通道:{},发生错误！", socketAddress, cause);
        }
    }
}
