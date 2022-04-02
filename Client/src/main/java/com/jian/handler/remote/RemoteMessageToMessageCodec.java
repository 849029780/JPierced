package com.jian.handler.remote;

import com.jian.beans.transfer.BaseTransferPacks;
import com.jian.beans.transfer.ConnectReqPacks;
import com.jian.beans.transfer.ConnectRespPacks;
import com.jian.commons.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@ChannelHandler.Sharable
public class RemoteMessageToMessageCodec extends MessageToMessageCodec<ByteBuf, BaseTransferPacks> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks, List<Object> out) throws Exception {
        int packSize = Constants.BASE_PACK_SIZE;
        byte type = baseTransferPacks.getType();
        ByteBuf buffer = ctx.alloc().buffer();
        switch (type) {
            case 1: { //连接请求
                ConnectReqPacks connectReqPacks = (ConnectReqPacks)baseTransferPacks;
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
                if(hostLen > 0){
                    buffer.writeBytes(hostBytes);
                }
                break;
            }
            case 2: {

                break;
            }
            case 3: {

                break;
            }
            case 4: {

                break;
            }
            case 5: {

                break;
            }
            case 6: {

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
                if(hostLen > 0){
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
            case 3: {

                break;
            }
            case 4: {

                break;
            }
            case 5: {

                break;
            }
            case 6: {

                break;
            }
        }
    }

}
