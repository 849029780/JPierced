package com.jian.handler.remote;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.start.Client;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
                TransferDataPacks transferDataPacks = (TransferDataPacks) baseTransferPacks;
                packSize += 8;
                ByteBuf datas = transferDataPacks.getDatas();
                int readableBytes = datas.readableBytes();
                packSize += readableBytes;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(transferDataPacks.getTargetChannelHash());
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
                int msgLen = byteBuf.readInt();
                if (msgLen > 0) {
                    ByteBuf msgBuff = byteBuf.readSlice(msgLen);
                    String msg = msgBuff.toString(StandardCharsets.UTF_8);
                    connectAuthRespPacks.setMsg(msg);
                }
                connectAuthRespPacks.setKey(key);
                connectAuthRespPacks.setState(state);
                connectAuthRespPacks.setMsgLen(msgLen);
                connectAuthRespPacks.setPackSize(packSize);
                list.add(connectAuthRespPacks);
            }
            case 7 -> { //传输数据
                TransferDataPacks transferDataPacks = new TransferDataPacks();
                long targetChannelHash = byteBuf.readLong();
                int readableBytes = byteBuf.readableBytes();
                ByteBuf buffer = byteBuf.readRetainedSlice(readableBytes);
                transferDataPacks.setTargetChannelHash(targetChannelHash);
                transferDataPacks.setDatas(buffer);
                list.add(transferDataPacks);
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
        Channel channel = ctx.channel();
        Boolean isAck = channel.attr(Constants.IS_ACK_CHANNEL_KEY).get();

        //如果不知通道类型，则跳过后续操作
        if (Objects.isNull(isAck)) {
            return;
        }

        //如果是ack通道，则需要关闭传输通道
        if (isAck) {
            if (Objects.nonNull(Constants.REMOTE_TRANSIMIT_CHANNEL)) {
                Constants.REMOTE_TRANSIMIT_CHANNEL.close();
            }
            //ack连接置空
            Constants.REMOTE_ACK_CHANNEL = null;
            return;
        }

        //如果关闭的是传输通道，则判断ack通道是否还连接，连接则需关闭
        if (Objects.nonNull(Constants.REMOTE_ACK_CHANNEL)) {
            Constants.REMOTE_ACK_CHANNEL.close();
        }

        if (Objects.nonNull(Constants.LOCAL_CHANNEL_MAP)) {
            //和服务端的连接断开，关闭本地所有的连接
            for (Map.Entry<Long, Channel> entry : Constants.LOCAL_CHANNEL_MAP.entrySet()) {
                Optional.ofNullable(entry.getValue()).ifPresent(ch -> ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE));
            }
            //本地连接关闭后置空
            Constants.LOCAL_CHANNEL_MAP = null;
        }
        Client client = channel.attr(Constants.CLIENT_KEY).get();
        Optional.ofNullable(client).ifPresent(cli -> {
            if (client.isCanReconnect()) {
                //重连
                Constants.CACHED_EXECUTOR_POOL.execute(cli::reConnct);
            } else {
                System.exit(0);
            }
        });
        //将连接置空
        Constants.REMOTE_TRANSIMIT_CHANNEL = null;
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
