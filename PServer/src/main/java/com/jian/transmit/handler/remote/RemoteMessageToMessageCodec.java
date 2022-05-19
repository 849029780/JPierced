package com.jian.transmit.handler.remote;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
            case 3 -> { //服务发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(disConnectReqPacks.getTarChannelHash());
                out.add(buffer);
            }
            case 5 -> { //认证结果响应
                ConnectAuthRespPacks connectAuthRespPacks = (ConnectAuthRespPacks) baseTransferPacks;
                packSize += 8 + 1 + 4;
                String msg = connectAuthRespPacks.getMsg();
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                int msgLen = msgBytes.length;
                if (msgLen > 0) {
                    packSize += msgLen;
                }
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectAuthRespPacks.getKey());
                buffer.writeByte(connectAuthRespPacks.getState());
                buffer.writeInt(msgLen);
                if (msgLen > 0) {
                    buffer.writeBytes(msgBytes);
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
            case 9 -> { //响应心跳
                HealthRespPacks healthRespPacks = (HealthRespPacks) baseTransferPacks;
                packSize += 8;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(healthRespPacks.getMsgId());
                out.add(buffer);
            }
            case 10 -> { //要求客户端断开连接
                DisConnectClientReqPacks disConnectClientReqPacks = (DisConnectClientReqPacks) baseTransferPacks;
                packSize += 1;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeByte(disConnectClientReqPacks.getCode());
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
            case 3 -> {//客户发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                long tarChannelHash = byteBuf.readLong();
                disConnectReqPacks.setTarChannelHash(tarChannelHash);
                list.add(disConnectReqPacks);
            }
            case 4 -> {//客户端认证请求
                ConnectAuthReqPacks connectAuthReqPacks = new ConnectAuthReqPacks();
                long key = byteBuf.readLong();
                int pwdLen = byteBuf.readInt();
                if (pwdLen > 0) {
                    ByteBuf pwdBuff = byteBuf.readSlice(pwdLen);
                    String pwd = pwdBuff.toString(StandardCharsets.UTF_8);
                    connectAuthReqPacks.setPwd(pwd);
                }
                connectAuthReqPacks.setPackSize(packSize);
                connectAuthReqPacks.setKey(key);
                connectAuthReqPacks.setPwdLen(pwdLen);
                list.add(connectAuthReqPacks);
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
            case 8 -> { //接收心跳请求
                HealthReqPacks healthReqPacks = new HealthReqPacks();
                long msgId = byteBuf.readLong();
                healthReqPacks.setMsgId(msgId);
                list.add(healthReqPacks);
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
                list.add(connectAckChannelReqPacks);
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
        //远程客户端有发生断开连接时，需要关闭该通道上的所有本地连接，且关闭当前客户端监听的端口
        Channel channel = ctx.channel();

        //如果是不知名通道，则直接不允许后面的操作
        Boolean isAck = channel.attr(Constants.IS_ACK_CHANNEL_KEY).get();
        if (Objects.isNull(isAck)) {
            return;
        }

        //如果关闭的是ack通道，则需要同时关闭传输通道
        if (isAck) {
            Channel remoteChannel = channel.attr(Constants.REMOTE_CHANNEL_IN_ACK_KEY).get();
            if (Objects.nonNull(remoteChannel)) {
                channel.attr(Constants.REMOTE_CHANNEL_IN_ACK_KEY).set(null);
                remoteChannel.close();
            }
            return;
        }

        //如果远程通道上有ack通道，则需要关闭ack通道
        Channel ackChannel = channel.attr(Constants.REMOTE_ACK_CHANNEL_KEY).get();
        if (Objects.nonNull(ackChannel)) {
            ackChannel.attr(Constants.REMOTE_CHANNEL_IN_ACK_KEY).set(null);
            channel.attr(Constants.REMOTE_ACK_CHANNEL_KEY).set(null);
            ackChannel.close();
        }

        //如果是关闭的传输通道，则本地端口上连接的通道，将这些通道关闭，然后再关闭端口监听
        Map<Long, Channel> localChannelMap = channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        Optional.ofNullable(localChannelMap).ifPresent(map -> {
            if (map.size() > 0) {
                for (Map.Entry<Long, Channel> entry : localChannelMap.entrySet()) {
                    if (Objects.nonNull(entry)) {
                        Channel localChannel = entry.getValue();
                        Optional.ofNullable(localChannel).ifPresent(ch -> ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE));
                    }
                }
            }
        });

        //关闭端口监听
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        Optional.ofNullable(clientInfo).ifPresent(cli -> {
            cli.setOnline(false);
            cli.setRemoteChannel(null);
            //获取该客户端监听的所有端口，并将这些端口全部取消监听
            Map<Integer, Channel> listenPortMap = clientInfo.getListenPortMap();
            log.warn("客户端key:{}，name:{}，断开连接,即将关闭该客户端监听的所有端口:{}", clientInfo.getKey(), clientInfo.getName(), listenPortMap.keySet());
            for (Map.Entry<Integer, Channel> listen : listenPortMap.entrySet()) {
                //监听的端口通道，非连接通道
                Channel listenChannel = listen.getValue();
                //关闭监听的端口
                Optional.ofNullable(listenChannel).ifPresent(ChannelOutboundInvoker::close);
                NetAddress netAddress = clientInfo.getPortMappingAddress().get(listen.getKey());
                Optional.ofNullable(netAddress).ifPresent(addr -> addr.setListen(false));
            }
            clientInfo.setListenPortMap(new ConcurrentHashMap<>());
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();
        boolean writable = channel.isWritable();
        //远程通道写缓冲状态和本地自动读状态设置为一致，如果通道缓冲写满了，则不允许本地通道自动读
        Map<Long, Channel> localChannelMap = channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        for (Map.Entry<Long, Channel> entry : localChannelMap.entrySet()) {
            Channel localChannel = entry.getValue();
            Optional.ofNullable(localChannel).ifPresent(ch -> ch.config().setAutoRead(writable));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        if (Objects.nonNull(clientInfo)) {
            if (cause instanceof SocketException cause1) {
                log.error("客户端通道发生错误！客户key:{},name:{},{}", clientInfo.getKey(), clientInfo.getName(), cause1.getMessage());
                return;
            }
            log.error("客户端通道发生错误！客户key:{},name:{}", clientInfo.getKey(), clientInfo.getName(), cause);
            return;
        }
        log.error("客户端通道发生错误！", cause);
    }
}
