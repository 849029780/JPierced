package com.jian.handler.remote;

import com.jian.start.ClientConnectInfo;
import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.handler.local.LocalListener;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
@ChannelHandler.Sharable
public class RemoteChannelInBoundHandler extends SimpleChannelInboundHandler<BaseTransferPacks> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks) throws Exception {
        byte type = baseTransferPacks.getType();
        Channel channel = ctx.channel();
        Map<Long, Channel> localChannelMap = channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        switch (type) {
            case 2: {//连接响应
                ConnectRespPacks connectRespPacks = (ConnectRespPacks) baseTransferPacks;
                Channel localChannel = localChannelMap.get(connectRespPacks.getThisChannelHash());

                if (Objects.isNull(localChannel)) {
                    log.warn("连接响应本地连接通道已不存在！");
                    return;
                }

                //连接成功
                if (connectRespPacks.getState().byteValue() == ConnectRespPacks.STATE.SUCCESS) {
                    //设置本地通道为自动读
                    localChannel.config().setAutoRead(Boolean.TRUE);
                } else {
                    //远程连接失败，则关闭本地的连接
                    localChannel.close();
                }
                break;
            }
            case 3: {//客户发起断开连接请求
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                Long tarChannelHash = disConnectReqPacks.getTarChannelHash();
                Channel localChannel = localChannelMap.remove(tarChannelHash);
                //关闭本地连接的通道
                Optional.ofNullable(localChannel).ifPresent(ChannelOutboundInvoker::close);
                break;
            }
            case 4: {//客户端认证请求
                ConnectAuthReqPacks connectAuthReqPacks = (ConnectAuthReqPacks) baseTransferPacks;
                Long key = connectAuthReqPacks.getKey();
                ClientConnectInfo clientConnectInfo = Constants.CLIENTS.get(key);
                if (Objects.isNull(clientConnectInfo)) {
                    ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
                    connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.FAIL);
                    connectAuthRespPacks.setKey(key);
                    connectAuthRespPacks.setMsg("当前key无权限连接..");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    return;
                }

                if (!clientConnectInfo.getPwd().equals(connectAuthReqPacks.getPwd())) {
                    ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
                    connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.FAIL);
                    connectAuthRespPacks.setKey(key);
                    connectAuthRespPacks.setMsg("当前key密码错误..");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    return;
                }


                clientConnectInfo.setOnline(true);
                clientConnectInfo.setRemoteChannel(channel);
                channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).set(new ConcurrentHashMap<>());
                channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).set(clientConnectInfo);


                //异步启动该客户端需要监听的端口
                Constants.FIXED_THREAD_POOL.execute(() -> {
                    Map<Integer, InetSocketAddress> portMappingAddress = clientConnectInfo.getPortMappingAddress();
                    new LocalListener().listen(portMappingAddress.keySet(), clientConnectInfo);
                });

                break;
            }
            case 7: { //传输数据
                TransferDataPacks transferDataPacks = (TransferDataPacks) baseTransferPacks;
                Long targetChannelHash = transferDataPacks.getTargetChannelHash();
                Channel localChannel = localChannelMap.get(targetChannelHash);
                Optional.ofNullable(localChannel).ifPresent(ch -> ch.writeAndFlush(transferDataPacks.getDatas()));
                break;
            }
        }
    }
}
