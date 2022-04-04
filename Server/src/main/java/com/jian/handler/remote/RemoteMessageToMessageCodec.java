package com.jian.handler.remote;

import com.jian.start.ClientConnectInfo;
import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
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
    protected void encode(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks, List<Object> out) throws Exception {
        int packSize = Constants.BASE_PACK_SIZE;
        byte type = baseTransferPacks.getType();
        ByteBuf buffer = ctx.alloc().buffer();
        switch (type) {
            case 1: { //服务发起连接请求
                ConnectReqPacks connectReqPacks = (ConnectReqPacks) baseTransferPacks;
                packSize += 8 + 8 + 4 + 4;
                byte[] hostBytes = connectReqPacks.getHost().getBytes(StandardCharsets.UTF_8);
                int hostLen = hostBytes.length;
                packSize += hostLen;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(connectReqPacks.getThisChannelHash());
                buffer.writeLong(connectReqPacks.getTarChannelHash());
                buffer.writeInt(connectReqPacks.getPort());
                buffer.writeInt(hostLen);
                if (hostLen > 0) {
                    buffer.writeBytes(hostBytes);
                }
                break;
            }
            case 3: { //服务发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                packSize += 8;
                buffer.writeInt(packSize);
                buffer.writeByte(type);
                buffer.writeLong(disConnectReqPacks.getTarChannelHash());
                break;
            }
            case 5: { //认证结果响应
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
            case 2: {//连接响应
                ConnectRespPacks connectRespPacks = new ConnectRespPacks();
                long thisChannelHash = byteBuf.readLong();
                byte state = byteBuf.readByte();
                int msgLen = byteBuf.readInt();

                connectRespPacks.setPackSize(packSize);
                connectRespPacks.setThisChannelHash(thisChannelHash);
                connectRespPacks.setState(state);
                connectRespPacks.setMsgLen(msgLen);
                if (msgLen > 0) {
                    ByteBuf msgBuf = byteBuf.readBytes(msgLen);
                    String msg = msgBuf.toString(StandardCharsets.UTF_8);
                    connectRespPacks.setMsg(msg);
                    ReferenceCountUtil.release(msgBuf);
                }
                list.add(connectRespPacks);
                break;
            }
            case 3: {//客户发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                long tarChannelHash = byteBuf.readLong();
                disConnectReqPacks.setTarChannelHash(tarChannelHash);
                list.add(disConnectReqPacks);
                break;
            }
            case 4: {//客户端认证请求
                ConnectAuthReqPacks connectAuthReqPacks = new ConnectAuthReqPacks();
                long key = byteBuf.readLong();
                int pwdLen = byteBuf.readInt();
                if (pwdLen > 0) {
                    ByteBuf pwdBuff = byteBuf.readBytes(pwdLen);
                    String pwd = pwdBuff.toString(StandardCharsets.UTF_8);
                    ReferenceCountUtil.release(pwdBuff);
                    connectAuthReqPacks.setPwd(pwd);
                }
                connectAuthReqPacks.setPackSize(packSize);
                connectAuthReqPacks.setKey(key);
                connectAuthReqPacks.setPwdLen(pwdLen);
                list.add(connectAuthReqPacks);
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
        //远程客户端有发生断开连接时，需要关闭该通道上的所有本地连接，且关闭当前客户端监听的端口
        Channel channel = ctx.channel();
        ClientConnectInfo clientConnectInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        Optional.ofNullable(clientConnectInfo).ifPresent(cli -> {
            //获取该客户端监听的所有端口，并将这些端口全部取消监听
            Map<Integer, Channel> listenPortMap = clientConnectInfo.getListenPortMap();
            log.info("客户端key:{}，断开连接,即将关闭该客户端监听的所有端口:{}", clientConnectInfo.getKey(), listenPortMap.keySet());
            cli.setOnline(false);
            cli.setRemoteChannel(null);


            Iterator<Map.Entry<Integer, Channel>> iterator = listenPortMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Channel> listen = iterator.next();
                //端口号
                Integer port = listen.getKey();
                //监听的端口通道，非连接通道
                Channel listenChannel = listen.getValue();
                //关闭监听的端口
                Optional.ofNullable(listenChannel).ifPresent(ChannelOutboundInvoker::close);
                //移除端口映射
                Constants.PORT_MAPPING_CLIENT.remove(port);
            }
            clientConnectInfo.setListenPortMap(new ConcurrentHashMap<>());
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Channel channel = ctx.channel();
        boolean writable = channel.isWritable();
        //远程通道写缓冲状态和本地自动读状态设置为一致，如果通道缓冲写满了，则不允许本地通道自动读
        Map<Long, Channel> localChannelMap = channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        Iterator<Map.Entry<Long, Channel>> iterator = localChannelMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Channel> entry = iterator.next();
            Channel localChannel = entry.getValue();
            Optional.ofNullable(localChannel).ifPresent(ch->ch.config().setAutoRead(writable));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        ClientConnectInfo clientConnectInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        log.error("远程通道发生错误,客户端key:{}", clientConnectInfo.getKey(), cause);
    }
}
