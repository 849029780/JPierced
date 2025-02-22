package com.jian.transmit.tcp.handler.remote.transfer;

import com.jian.beans.transfer.*;
import com.jian.beans.transfer.beans.NetAddr;
import com.jian.beans.transfer.req.*;
import com.jian.beans.transfer.resp.ConnectAuthRespPacks;
import com.jian.beans.transfer.resp.HealthRespPacks;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageCodec;
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
                packSize += 8 + 1 + 4 + 4;
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
                buffer.writeInt(connectAuthRespPacks.getAckPort());
                buffer.writeInt(msgLen);
                if (msgLen > 0) {
                    buffer.writeBytes(msgBytes);
                }
                out.add(buffer);
            }
            case 7 -> { //传输数据
                TcpTransferDataPacks tcpTransferDataPacks = (TcpTransferDataPacks) baseTransferPacks;
                packSize += 8;
                ByteBuf data = tcpTransferDataPacks.getDatas();
                int readableBytes = data.readableBytes();
                packSize += readableBytes;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(tcpTransferDataPacks.getTarChannelHash());
                out.add(buffer);
                out.add(data);
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
            case 20 -> { //udp数据传输
                UdpTransferDataPacks udpTransferDataPacks = (UdpTransferDataPacks) baseTransferPacks;
                ByteBuf data = udpTransferDataPacks.getDatas();

                String senderHost = udpTransferDataPacks.getSenderHost();
                byte[] bytes = senderHost.getBytes(StandardCharsets.UTF_8);

                int dataLen = data.readableBytes();
                packSize += 4 + 1 + bytes.length + 4 + 4 + dataLen;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeInt(udpTransferDataPacks.getSourcePort());



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
            case 7 -> { //tcp传输数据
                TcpTransferDataPacks tcpTransferDataPacks = new TcpTransferDataPacks();
                long targetChannelHash = byteBuf.readLong();
                int readableBytes = byteBuf.readableBytes();
                ByteBuf buffer = byteBuf.readRetainedSlice(readableBytes);
                tcpTransferDataPacks.setTarChannelHash(targetChannelHash);
                tcpTransferDataPacks.setDatas(buffer);
                list.add(tcpTransferDataPacks);
            }
            case 8 -> { //接收心跳请求
                HealthReqPacks healthReqPacks = new HealthReqPacks();
                long msgId = byteBuf.readLong();
                healthReqPacks.setMsgId(msgId);
                list.add(healthReqPacks);
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
        //远程客户端有发生断开连接时，需要关闭该通道上的所有本地连接，且关闭当前客户端监听的端口
        Channel channel = ctx.channel();
        log.debug("传输通道关闭...");

        //关闭端口监听
        ClientInfo clientInfo = channel.attr(Constants.CLIENT_INFO_KEY).get();
        if (Objects.isNull(clientInfo)) return;

        //如果是关闭的传输通道，则本地端口上连接的通道，将这些通道关闭，然后再关闭端口监听
        ConcurrentHashMap<Long, Channel> connectedMap = clientInfo.getConnectedMap();

        Optional.ofNullable(connectedMap).ifPresent(map -> {
            if (!map.isEmpty()) {
                for (Map.Entry<Long, Channel> entry : connectedMap.entrySet()) {
                    if (Objects.nonNull(entry)) {
                        Channel localChannel = entry.getValue();
                        Optional.ofNullable(localChannel).ifPresent(ch -> ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE));
                    }
                }
            }
        });


        clientInfo.setOnline(false);
        clientInfo.setRemoteChannel(null);

        //如果远程通道上有ack通道，则需要关闭ack通道
        Channel ackChannel = clientInfo.getAckChannel();
        if (Objects.nonNull(ackChannel)) {
            ackChannel.close();
        }

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


    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();
        boolean writable = channel.isWritable();
        //远程通道写缓冲状态和本地自动读状态设置为一致，如果通道缓冲写满了，则不允许本地通道自动读
        ClientInfo clientInfo = channel.attr(Constants.CLIENT_INFO_KEY).get();
        if (Objects.isNull(clientInfo)) return;
        ConcurrentHashMap<Long, Channel> connectedMap = clientInfo.getConnectedMap();
        for (Map.Entry<Long, Channel> entry : connectedMap.entrySet()) {
            Channel localChannel = entry.getValue();
            Optional.ofNullable(localChannel).ifPresent(ch -> ch.config().setAutoRead(writable));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = channel.attr(Constants.CLIENT_INFO_KEY).get();
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
