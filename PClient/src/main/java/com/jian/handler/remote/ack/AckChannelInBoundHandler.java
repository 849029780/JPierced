package com.jian.handler.remote.ack;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.start.Client;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
@ChannelHandler.Sharable
public class AckChannelInBoundHandler extends SimpleChannelInboundHandler<BaseTransferPacks> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks) {
        byte type = baseTransferPacks.getType();
        Channel channel = ctx.channel();
        switch (type) {
            case 1 -> {//连接请求
                ConnectReqPacks connectReqPacks = (ConnectReqPacks) baseTransferPacks;
                String host = connectReqPacks.getHost();
                Integer port = connectReqPacks.getPort();
                Long thisChannelHash = connectReqPacks.getThisChannelHash();
                long tarChannelHash = connectReqPacks.getTarChannelHash();

                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                log.debug("连接本地服务:{}:{}，协议类型:{}", host, port, connectReqPacks.getProtocol());
                Client client;
                if (connectReqPacks.getProtocol() == ConnectReqPacks.Protocol.HTTPS) {
                    client = Client.getLocalHttpsInstance().option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                } else {
                    client = Client.getLocalInstance().option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                }

                //发起连接本地
                client.connect(inetSocketAddress, (GenericFutureListener<ChannelFuture>) null).addListener(future -> {
                    ChannelFuture channelFuture = (ChannelFuture) future;
                    ConnectRespPacks connectRespPacks = new ConnectRespPacks();
                    connectRespPacks.setThisChannelHash(tarChannelHash);
                    connectRespPacks.setTarChannelHash(thisChannelHash);
                    if (channelFuture.isSuccess()) {
                        Channel localChannel = channelFuture.channel();
                        //暂设置本地连接为不可读
                        localChannel.config().setAutoRead(Boolean.FALSE);
                        localChannel.attr(Constants.THIS_CHANNEL_HASH_KEY).set(thisChannelHash);
                        localChannel.attr(Constants.TAR_CHANNEL_HASH_KEY).set(tarChannelHash);
                        if (Objects.isNull(Constants.LOCAL_CHANNEL_MAP)) {
                            Constants.LOCAL_CHANNEL_MAP = new ConcurrentHashMap<>();
                        }
                        Constants.LOCAL_CHANNEL_MAP.put(thisChannelHash, localChannel);
                        localChannel.config().setAutoRead(Boolean.TRUE);
                        connectRespPacks.setState(ConnectRespPacks.STATE.SUCCESS);
                        connectRespPacks.setMsg("连接本地服务成功！");
                        log.debug("连接本地服务:{}:{}成功！", host, port);
                    } else {
                        connectRespPacks.setState(ConnectRespPacks.STATE.FAIL);
                        connectRespPacks.setMsg("连接本地服务失败！请检查地址和端口再试！");
                        String causeMsg = "";
                        if (Objects.nonNull(channelFuture.cause())) {
                            causeMsg = channelFuture.cause().getMessage();
                        }
                        log.warn("连接本地服务:{}:{}失败！{}", host, port, causeMsg);
                    }
                    //连接响应
                    ctx.writeAndFlush(connectRespPacks);
                });
            }
            case 11 -> { //消息处理
                MessageReqPacks messageReqPacks = (MessageReqPacks) baseTransferPacks;
                int msgLen = messageReqPacks.getMsgLen();
                String msg = messageReqPacks.getMsg();
                log.info("接收到服务端消息，消息长度：{}，内容:{}", msgLen, msg);
            }
            case 13 -> { //ack连接响应
                ConnectAckChannelRespPacks connectAckChannelRespPacks = (ConnectAckChannelRespPacks) baseTransferPacks;
                byte state = connectAckChannelRespPacks.getState();
                String msg = connectAckChannelRespPacks.getMsg();
                if (state == BaseTransferPacks.STATE.FAIL) {
                    log.info("ack连接失败！{}", msg);
                    channel.close();
                    //关闭传输通道，重新发起连接
                    Constants.REMOTE_TRANSIMIT_CHANNEL.close();
                    return;
                }
                //连接成功
                Constants.REMOTE_ACK_CHANNEL = channel;

                //---------------------传输通道后续设置------------------
                Channel transimitChannel = Constants.REMOTE_TRANSIMIT_CHANNEL;
                //是否已开启心跳
                Boolean isOpenHealth = transimitChannel.attr(Constants.HEALTH_IS_OPEN_KEY).get();
                if (Objects.isNull(isOpenHealth)) {
                    log.info("ack连接完成！{}心跳已开启..", msg);
                    transimitChannel.pipeline().addFirst(new IdleStateHandler(0, Constants.HEART_DELAY, Constants.DISCONNECT_HEALTH_SECONDS, TimeUnit.SECONDS));
                    transimitChannel.attr(Constants.HEALTH_IS_OPEN_KEY).set(Boolean.TRUE);
                } else {
                    log.info("ack连接完成！{}", msg);
                }

                Client client = transimitChannel.attr(Constants.CLIENT_KEY).get();
                //连接成功后才允许断开重连
                Optional.ofNullable(client).ifPresent(cli -> {
                    //可进行重连
                    client.setCanReconnect(true);
                    //重连次数置空
                    //client.getRecount().set(0);
                });
                //---------------------传输通道后续设置------------------
            }
            case 14 -> { //ack通道消息-设置是否自动读
                AutoreadReqPacks autoreadReqPacks = (AutoreadReqPacks) baseTransferPacks;
                Channel localChannel = Constants.LOCAL_CHANNEL_MAP.get(autoreadReqPacks.getTarChannelHash());
                Optional.ofNullable(localChannel).ifPresent(ch -> ch.config().setAutoRead(autoreadReqPacks.isAutoRead()));
            }
        }
    }
}
