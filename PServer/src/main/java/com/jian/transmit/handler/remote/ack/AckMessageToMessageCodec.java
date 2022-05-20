package com.jian.transmit.handler.remote.ack;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

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
            case 1 -> { //服务发起连接请求
                ConnectReqPacks connectReqPacks = (ConnectReqPacks) baseTransferPacks;
                packSize += 8 + 8 + 4 + 1 + 4;
                byte[] hostBytes = connectReqPacks.getHost().getBytes(StandardCharsets.UTF_8);
                int hostLen = hostBytes.length;
                packSize += hostLen;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectReqPacks.getThisChannelHash());
                buffer.writeLong(connectReqPacks.getTarChannelHash());
                buffer.writeInt(connectReqPacks.getPort());
                buffer.writeByte(connectReqPacks.getProtocol());
                buffer.writeInt(hostLen);
                if (hostLen > 0) {
                    buffer.writeBytes(hostBytes);
                }
                out.add(buffer);
            }
            case 11 -> { //发送消息
                MessageReqPacks messageReqPacks = (MessageReqPacks) baseTransferPacks;
                packSize += 4;
                String msg = messageReqPacks.getMsg();
                byte[] msgBytes = null;
                int msgLen = 0;
                if (!StringUtil.isNullOrEmpty(msg)) {
                    msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                    msgLen = msgBytes.length;
                    packSize += msgLen;
                }
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeInt(msgLen);
                if (Objects.nonNull(msgBytes)) {
                    buffer.writeBytes(msgBytes);
                }
                out.add(buffer);
            }
            case 13 -> { //ack通道连接响应
                ConnectAckChannelRespPacks connectAckChannelRespPacks = (ConnectAckChannelRespPacks) baseTransferPacks;
                packSize += 1 + 4;
                String msg = connectAckChannelRespPacks.getMsg();
                byte[] msgBytes = null;
                int msgLen = 0;
                if (!StringUtil.isNullOrEmpty(msg)) {
                    msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                    msgLen = msgBytes.length;
                    packSize += msgLen;
                }
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeByte(connectAckChannelRespPacks.getState());
                buffer.writeInt(msgLen);
                if (Objects.nonNull(msgBytes)) {
                    buffer.writeBytes(msgBytes);
                }
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
            case 2 -> {//连接响应
                ConnectRespPacks connectRespPacks = new ConnectRespPacks();
                long thisChannelHash = byteBuf.readLong();
                long tarChannelHash = byteBuf.readLong();
                byte state = byteBuf.readByte();
                int msgLen = byteBuf.readInt();

                connectRespPacks.setPackSize(packSize);
                connectRespPacks.setThisChannelHash(thisChannelHash);
                connectRespPacks.setTarChannelHash(tarChannelHash);
                connectRespPacks.setState(state);
                connectRespPacks.setMsgLen(msgLen);
                if (msgLen > 0) {
                    ByteBuf msgBuf = byteBuf.readSlice(msgLen);
                    String msg = msgBuf.toString(StandardCharsets.UTF_8);
                    connectRespPacks.setMsg(msg);
                }
                list.add(connectRespPacks);
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
            case 12 -> { //活动通道连接
                ConnectAckChannelReqPacks connectAckChannelReqPacks = new ConnectAckChannelReqPacks();
                long key = byteBuf.readLong();
                connectAckChannelReqPacks.setKey(key);
                connectAckChannelReqPacks.setPackSize(packSize);
                list.add(connectAckChannelReqPacks);
            }
            case 14 -> { //ack通道-设置自动读
                AutoreadReqPacks autoreadReqPacks = new AutoreadReqPacks();
                long tarChannelHash = byteBuf.readLong();
                boolean isAutoRead = byteBuf.readBoolean();
                autoreadReqPacks.setPackSize(packSize);
                autoreadReqPacks.setTarChannelHash(tarChannelHash);
                autoreadReqPacks.setAutoRead(isAutoRead);
                list.add(autoreadReqPacks);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        //远程客户端有发生断开连接时，需要关闭该通道上的所有本地连接，且关闭当前客户端监听的端口
        Channel channel = ctx.channel();
        log.info("ack通道断开...");
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        if (Objects.nonNull(clientInfo)) {
            Channel remoteChannel = clientInfo.getRemoteChannel();
            if (Objects.nonNull(remoteChannel)) {
                clientInfo.setAckChannel(null);
                remoteChannel.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        if (Objects.nonNull(clientInfo)) {
            if (cause instanceof SocketException cause1) {
                log.error("客户端ack通道发生错误！客户key:{},name:{},{}", clientInfo.getKey(), clientInfo.getName(), cause1.getMessage());
                return;
            }
            log.error("客户端ack通道发生错误！客户key:{},name:{}", clientInfo.getKey(), clientInfo.getName(), cause);
            return;
        }
        log.error("客户端ack通道发生错误！", cause);
    }
}
