package com.jian.handler.remote;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.start.Client;
import io.netty.channel.*;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;

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
        switch (type) {
            case 1: {//连接请求
                ConnectReqPacks connectReqPacks = (ConnectReqPacks) baseTransferPacks;
                String host = connectReqPacks.getHost();
                Integer port = connectReqPacks.getPort();
                Long thisChannelHash = connectReqPacks.getThisChannelHash();
                Long tarChannelHash = connectReqPacks.getTarChannelHash();

                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                log.info("连接本地服务:{}:{}", host, port);
                //发起连接本地
                Client.getLocalInstance().connect(inetSocketAddress, (GenericFutureListener) null).addListener(future -> {
                    ChannelFuture channelFuture = (ChannelFuture) future;
                    ConnectRespPacks connectRespPacks = new ConnectRespPacks();
                    connectRespPacks.setThisChannelHash(tarChannelHash);
                    if (channelFuture.isSuccess()) {
                        Channel localChannel = channelFuture.channel();
                        //暂设置本地连接为不可读
                        localChannel.config().setAutoRead(Boolean.FALSE);
                        localChannel.attr(Constants.THIS_CHANNEL_HASH_KEY).set(thisChannelHash);
                        localChannel.attr(Constants.TAR_CHANNEL_HASH_KEY).set(tarChannelHash);
                        Constants.LOCAL_CHANNEL_MAP.put(thisChannelHash, localChannel);
                        localChannel.config().setAutoRead(Boolean.TRUE);
                        connectRespPacks.setState(ConnectRespPacks.STATE.SUCCESS);
                        connectRespPacks.setMsg("连接本地服务成功！");
                        log.info("连接本地服务:{}:{}成功！", host, port);
                    } else {
                        connectRespPacks.setState(ConnectRespPacks.STATE.FAIL);
                        connectRespPacks.setMsg("连接本地服务失败！请检查地址和端口再试！");
                        log.warn("连接本地服务:{}:{}失败！", host, port, channelFuture.cause());
                    }
                    Constants.REMOTE_CHANNEL.writeAndFlush(connectRespPacks);
                });
                break;
            }
            case 3: {//断开连接
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                Long tarChannelHash = disConnectReqPacks.getTarChannelHash();
                Channel localChannel = Constants.LOCAL_CHANNEL_MAP.remove(tarChannelHash);
                Optional.ofNullable(localChannel).ifPresent(ChannelOutboundInvoker::close);
                log.info("接收到远程发起断开本地连接请求,tarChannelHash:{}", tarChannelHash);
                break;
            }
            case 5: { //认证响应
                ConnectAuthRespPacks connectAuthRespPacks = (ConnectAuthRespPacks) baseTransferPacks;
                Byte state = connectAuthRespPacks.getState();
                String msg = connectAuthRespPacks.getMsg();
                if (!StringUtil.isNullOrEmpty(msg)) {
                    log.info(msg);
                }
                if (state.byteValue() == ConnectRespPacks.STATE.SUCCESS) {
                    //连接成功后保存通道，并重置该连接的重试次数为0，只要连接上后都断连都将重试
                    if (Objects.isNull(Constants.REMOTE_CHANNEL)) {
                        Constants.REMOTE_CHANNEL = channel;
                    }
                    Client client = channel.attr(Constants.CLIENT_KEY).get();
                    Optional.ofNullable(client).ifPresent(cli -> client.getRecount().set(0));
                }
                break;
            }
            case 7: { //传输数据
                TransferDataPacks transferDataPacks = ((TransferDataPacks) baseTransferPacks);
                Long targetChannelHash = transferDataPacks.getTargetChannelHash();
                Channel localChannel = Constants.LOCAL_CHANNEL_MAP.get(targetChannelHash);
                Optional.ofNullable(localChannel).ifPresent(ch -> ch.writeAndFlush(transferDataPacks.getDatas()));
                break;
            }
        }
    }
}
