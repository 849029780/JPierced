package com.jian.handler.remote;

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
import java.net.SocketAddress;
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
public class RemoteChannelInBoundHandler extends SimpleChannelInboundHandler<BaseTransferPacks> {

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
                Long tarChannelHash = connectReqPacks.getTarChannelHash();

                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                log.debug("连接本地服务:{}:{}，协议类型:{}", host, port, connectReqPacks.getProtocol());
                Client client;
                if (connectReqPacks.getProtocol() == ConnectReqPacks.Protocol.HTTPS) {
                    client = Client.getLocalHttpsInstance();
                } else {
                    client = Client.getLocalInstance();
                }
                //发起连接本地
                client.connect(inetSocketAddress, (GenericFutureListener) null).addListener(future -> {
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
                    Constants.REMOTE_TRANSIMIT_CHANNEL.writeAndFlush(connectRespPacks);
                });
            }
            case 3 -> {//断开连接
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                Long tarChannelHash = disConnectReqPacks.getTarChannelHash();
                Optional.ofNullable(Constants.LOCAL_CHANNEL_MAP).ifPresent(map -> {
                    Channel localChannel = map.remove(tarChannelHash);
                    Optional.ofNullable(localChannel).ifPresent(localCh -> localCh.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE));
                });
                log.debug("接收到远程发起断开本地连接请求,tarChannelHash:{}", tarChannelHash);
            }
            case 5 -> { //认证响应
                ConnectAuthRespPacks connectAuthRespPacks = (ConnectAuthRespPacks) baseTransferPacks;
                byte state = connectAuthRespPacks.getState();
                String msg = connectAuthRespPacks.getMsg();
                if (state == ConnectRespPacks.STATE.SUCCESS) {
                    //连接成功后保存通道，并重置该连接的重试次数为0，只要连接上后都断连都将重试
                    if (Objects.isNull(Constants.REMOTE_TRANSIMIT_CHANNEL)) {
                        Constants.REMOTE_TRANSIMIT_CHANNEL = channel;
                    }
                    log.info("连接服务已认证，发起ack连接..");
                    //ack连接
                    //服务端地址
                    InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
                    Client.getRemoteInstance().connect(remoteAddress, (Consumer<ChannelFuture>) future -> {
                        Channel ackChannel = future.channel();
                        ackChannel.attr(Constants.IS_ACK_CHANNEL_KEY).set(Boolean.TRUE);
                        String keyStr = Constants.CONFIG.getProperty(Constants.KEY_PROPERTY_NAME);
                        ConnectAckChannelReqPacks connectAckChannelReqPacks = new ConnectAckChannelReqPacks();
                        connectAckChannelReqPacks.setKey(Long.parseLong(keyStr));
                        ackChannel.writeAndFlush(connectAckChannelReqPacks);
                    });
                } else {
                    log.warn("连接服务认证失败！{}", msg);
                    channel.close();
                }
            }
            case 7 -> { //传输数据
                TransferDataPacks transferDataPacks = ((TransferDataPacks) baseTransferPacks);
                Long targetChannelHash = transferDataPacks.getTargetChannelHash();
                Channel localChannel = Constants.LOCAL_CHANNEL_MAP.get(targetChannelHash);
                ByteBuf datas = transferDataPacks.getDatas();
                try {
                    localChannel.writeAndFlush(datas);
                } catch (NullPointerException e) {
                    ReferenceCountUtil.release(datas);
                }
            }
            case 9 -> { //心跳响应
                HealthRespPacks healthRespPacks = (HealthRespPacks) baseTransferPacks;
                Long msgId = healthRespPacks.getMsgId();
                log.info("接收到服务端的心跳响应，msgId:{}", msgId);
            }
            case 10 -> {//断开和远程的连接
                log.debug("接收到服务端要求断开和服务端的连接！");
                Client client = channel.attr(Constants.CLIENT_KEY).get();
                //连接成功后才会将重试次数置空
                Optional.ofNullable(client).ifPresent(cli -> {
                    //设置不可进行重连
                    client.setCanReconnect(false);
                });
                channel.close();
            }
            case 11 -> {
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


                //传输通道相关设置
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

            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE: //读空闲
                    // 不处理
                    break;
                case WRITER_IDLE: //写空闲
                    long msgId = System.nanoTime();
                    log.info("发送心跳请求，msgId:{}", msgId);
                    HealthReqPacks healthReqPacks = new HealthReqPacks();
                    healthReqPacks.setMsgId(msgId);
                    ctx.writeAndFlush(healthReqPacks);
                    // 不处理
                    break;
                case ALL_IDLE: //读写空闲
                    // 记录上一次接收到心跳的时间，如果时间超过规定阈值则该连接已经断开
                    log.warn("接收服务端响应的心跳超时！已自动认为该连接已断开，准备关闭本地连接...");
                    //这里close后会自动出发channelRemove事件，然后执行关闭本地相关端口连接
                    ctx.close();
                    break;
            }
        }
    }
}
