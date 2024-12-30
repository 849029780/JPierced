package com.jian.transmit.tcp.handler.remote.ack;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
@ChannelHandler.Sharable
public class AckMessageToMessageCodec extends MessageToMessageCodec<ByteBuf, BaseTransferPacks> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks, List<Object> out) {
        int packSize = Constants.BASE_PACK_SIZE;
        byte type = baseTransferPacks.getType();
        ByteBuf buffer = ctx.alloc().buffer();
        switch (type) {
            case 2 -> { //连接请求响应
                ConnectRespPacks connectRespPacks = (ConnectRespPacks) baseTransferPacks;
                packSize += 8 + 8 + 1 + 4;
                String msg = connectRespPacks.getMsg();
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                int msgLen = msgBytes.length;
                packSize += msgLen;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectRespPacks.getThisChannelHash());
                buffer.writeLong(connectRespPacks.getTarChannelHash());
                buffer.writeByte(connectRespPacks.getState());
                buffer.writeInt(msgLen);
                if (msgLen > 0) {
                    buffer.writeBytes(msgBytes);
                }
                out.add(buffer);
            }
            case 8 -> {//心跳请求
                HealthReqPacks healthReqPacks = (HealthReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(healthReqPacks.getMsgId());
                out.add(buffer);
            }
            case 11 -> { //发送消息
                MessageReqPacks messageReqPacks = (MessageReqPacks) baseTransferPacks;
                packSize += 4;
                String msg = messageReqPacks.getMsg();
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                int msgLen = msgBytes.length;
                packSize += msgLen;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeInt(msgLen);
                buffer.writeBytes(msgBytes);
                out.add(buffer);
            }
            case 12 -> { //ack连接请求
                ConnectAckChannelReqPacks connectAckChannelReqPacks = (ConnectAckChannelReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectAckChannelReqPacks.getKey());
                out.add(buffer);
            }
            case 14 -> { //ack通道-设置自动读
                AutoreadReqPacks autoreadReqPacks = (AutoreadReqPacks) baseTransferPacks;
                packSize += 1 + 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(autoreadReqPacks.getTarChannelHash());
                buffer.writeBoolean(autoreadReqPacks.isAutoRead());
                out.add(buffer);
            }
        }

    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        int packSize = byteBuf.readInt();
        byte type = byteBuf.readByte();
        switch (type) {
            case 1 -> {//连接请求
                ConnectReqPacks connectRespPacks = new ConnectReqPacks();
                long thisChannelHash = byteBuf.readLong();
                long tarChannelHash = byteBuf.readLong();
                int port = byteBuf.readInt();
                byte protocol = byteBuf.readByte();
                int hostLen = byteBuf.readInt();
                if (hostLen > 0) {
                    ByteBuf hostBuff = byteBuf.readSlice(hostLen);
                    String host = hostBuff.toString(StandardCharsets.UTF_8);
                    connectRespPacks.setHost(host);
                }
                connectRespPacks.setPackSize(packSize);
                connectRespPacks.setThisChannelHash(thisChannelHash);
                connectRespPacks.setTarChannelHash(tarChannelHash);
                connectRespPacks.setPort(port);
                connectRespPacks.setProtocol(protocol);
                list.add(connectRespPacks);
            }
            case 9 -> {//心跳响应
                HealthRespPacks healthRespPacks = new HealthRespPacks();
                long msgId = byteBuf.readLong();
                healthRespPacks.setMsgId(msgId);
                list.add(healthRespPacks);
            }
            case 11 -> { //解消息
                MessageReqPacks messageReqPacks = new MessageReqPacks();
                int msgLen = byteBuf.readInt();
                if (msgLen > 0) {
                    String msg = byteBuf.readSlice(msgLen).toString(StandardCharsets.UTF_8);
                    messageReqPacks.setMsg(msg);
                }
                messageReqPacks.setMsgLen(msgLen);
                list.add(messageReqPacks);
            }
            case 13 -> { //ack连接响应
                ConnectAckChannelRespPacks connectAckChannelRespPacks = new ConnectAckChannelRespPacks();
                byte state = byteBuf.readByte();
                connectAckChannelRespPacks.setState(state);
                int msgLen = byteBuf.readInt();
                if (msgLen > 0) {
                    String msg = byteBuf.readSlice(msgLen).toString(StandardCharsets.UTF_8);
                    connectAckChannelRespPacks.setMsg(msg);
                }
                list.add(connectAckChannelRespPacks);
            }
            case 14 -> { //ack通道-设置自动读
                AutoreadReqPacks autoreadReqPacks = new AutoreadReqPacks();
                long tarChannelHash = byteBuf.readLong();
                boolean isAutoRead = byteBuf.readBoolean();
                autoreadReqPacks.setTarChannelHash(tarChannelHash);
                autoreadReqPacks.setAutoRead(isAutoRead);
                list.add(autoreadReqPacks);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("ack连接断开...");
        if (Objects.nonNull(Constants.REMOTE_TRANSIMIT_CHANNEL)) {
            Constants.REMOTE_TRANSIMIT_CHANNEL.close();
        }
        //ack连接置空
        Constants.REMOTE_ACK_CHANNEL = null;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        if (cause instanceof SocketException) {
            log.error("远程ack通道:{},发生错误！{}", socketAddress, cause.getMessage());
        } else {
            log.error("远程ack通道:{},发生错误！", socketAddress, cause);
        }
    }
}
