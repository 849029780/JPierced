package com.jian.handler.remote;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.start.Client;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
@ChannelHandler.Sharable
public class RemoteMessageToMessageCodec extends MessageToMessageCodec<ByteBuf, BaseTransferPacks> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks, List<Object> out) throws Exception {
        int packSize = Constants.BASE_PACK_SIZE;
        byte type = baseTransferPacks.getType();
        ByteBuf buffer = ctx.alloc().buffer();
        switch (type) {
            case 2: { //连接请求响应
                ConnectRespPacks connectRespPacks = (ConnectRespPacks) baseTransferPacks;
                packSize += 8 + 1 + 4;
                String msg = connectRespPacks.getMsg();
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                int msgLen = msgBytes.length;
                packSize += msgLen;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectRespPacks.getThisChannelHash());
                buffer.writeByte(connectRespPacks.getState());
                buffer.writeInt(msgLen);
                if (msgLen > 0) {
                    buffer.writeBytes(msgBytes);
                }
                break;
            }
            case 3: { //发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(disConnectReqPacks.getTarChannelHash());
                break;
            }
            case 4: { //发起认证
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
                break;
            }
            case 7: { //传输数据
                TransferDataPacks transferDataPacks = (TransferDataPacks) baseTransferPacks;
                packSize += 8;
                ByteBuf datas = transferDataPacks.getDatas();
                int readableBytes = datas.readableBytes();
                packSize += readableBytes;

                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(transferDataPacks.getTargetChannelHash());
                buffer.writeBytes(datas);
                break;
            }
        }
        out.add(buffer);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        int packSize = byteBuf.readInt();
        byte type = byteBuf.readByte();
        switch (type) {
            case 1: {//连接响应
                ConnectReqPacks connectRespPacks = new ConnectReqPacks();
                long thisChannelHash = byteBuf.readLong();
                long tarChannelHash = byteBuf.readLong();
                int port = byteBuf.readInt();
                int hostLen = byteBuf.readInt();
                if (hostLen > 0) {
                    ByteBuf hostBuff = byteBuf.readBytes(hostLen);
                    String host = hostBuff.toString(StandardCharsets.UTF_8);
                    ReferenceCountUtil.release(hostBuff);
                    connectRespPacks.setHost(host);
                }
                connectRespPacks.setPackSize(packSize);
                connectRespPacks.setThisChannelHash(thisChannelHash);
                connectRespPacks.setTarChannelHash(tarChannelHash);
                connectRespPacks.setPort(port);
                list.add(connectRespPacks);
                break;
            }
            case 3: {//发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                long tarChannelHash = byteBuf.readLong();
                disConnectReqPacks.setTarChannelHash(tarChannelHash);
                list.add(disConnectReqPacks);
                break;
            }
            case 5: {
                ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
                long key = byteBuf.readLong();
                byte state = byteBuf.readByte();
                int msgLen = byteBuf.readInt();
                if (msgLen > 0) {
                    ByteBuf msgBuff = byteBuf.readBytes(msgLen);
                    String msg = msgBuff.toString(StandardCharsets.UTF_8);
                    ReferenceCountUtil.release(msgBuff);
                    connectAuthRespPacks.setMsg(msg);
                }
                connectAuthRespPacks.setKey(key);
                connectAuthRespPacks.setState(state);
                connectAuthRespPacks.setMsgLen(msgLen);
                connectAuthRespPacks.setPackSize(packSize);
                list.add(connectAuthRespPacks);
                break;
            }
            case 7: { //传输数据
                TransferDataPacks transferDataPacks = new TransferDataPacks();
                long targetChannelHash = byteBuf.readLong();
                int readableBytes = byteBuf.readableBytes();
                ByteBuf buffer = channelHandlerContext.alloc().buffer(readableBytes);
                buffer.writeBytes(byteBuf);
                transferDataPacks.setTargetChannelHash(targetChannelHash);
                transferDataPacks.setDatas(buffer);
                list.add(transferDataPacks);
                break;
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        //和服务端的连接断开，关闭本地所有的连接
        Iterator<Map.Entry<Long, Channel>> iterator = Constants.LOCAL_CHANNEL_MAP.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Channel> entry = iterator.next();
            Optional.ofNullable(entry.getValue()).ifPresent(ChannelOutboundInvoker::close);
        }
        //将连接置空
        Constants.REMOTE_CHANNEL = null;
        //本地连接关闭后置空
        Constants.LOCAL_CHANNEL_MAP = new ConcurrentHashMap<>();

        log.warn("与服务端的连接断开，本地所有连接已关闭..{}s后将发起重连..", Constants.RECONNECT_DELAY);
        Client client = channel.attr(Constants.CLIENT_KEY).get();
        Optional.ofNullable(client).ifPresent(cli->cli.reConnct());
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();
        boolean writable = channel.isWritable();
        //远程通道写缓冲状态和本地自动读状态设置为一致，如果通道缓冲写满了，则不允许本地通道自动读
        Iterator<Map.Entry<Long, Channel>> iterator = Constants.LOCAL_CHANNEL_MAP.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Channel> entry = iterator.next();
            Channel localChannel = entry.getValue();
            Optional.ofNullable(localChannel).ifPresent(ch->ch.config().setAutoRead(writable));
        }
    }
}
