package com.jian.handler.remote;

import com.jian.beans.transfer.BaseTransferPacks;
import com.jian.beans.transfer.ConnectRespPacks;
import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class RemoteChannelInBoundHandler extends SimpleChannelInboundHandler<BaseTransferPacks> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks) throws Exception {
        byte type = baseTransferPacks.getType();
        Channel channel = ctx.channel();
        switch (type) {
            case 2: {//连接响应
                ConnectRespPacks connectRespPacks = (ConnectRespPacks)baseTransferPacks;
                Map<Long, Channel> localChannelMap = channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
                Channel localChannel = localChannelMap.get(connectRespPacks.getThisChannelHash());

                if(Objects.isNull(localChannel)){
                    log.warn("连接响应本地连接通道已不存在！");
                    return;
                }
                //连接成功
                if (connectRespPacks.getState().byteValue() == ConnectRespPacks.STATE.SUCCESS) {
                    //设置本地通道为自动读
                    localChannel.config().setAutoRead(Boolean.TRUE);
                }else{
                    //远程连接失败，则关闭本地的连接
                    localChannel.close();
                }
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
