package com.jian.handler.remote;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.start.Client;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
                        if (Objects.isNull(Constants.LOCAL_CHANNEL_MAP)) {
                            Constants.LOCAL_CHANNEL_MAP = new ConcurrentHashMap<>();
                        }
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
                Optional.ofNullable(Constants.LOCAL_CHANNEL_MAP).ifPresent(map -> {
                    Channel localChannel = map.remove(tarChannelHash);
                    Optional.ofNullable(localChannel).ifPresent(ChannelOutboundInvoker::close);
                });
                log.info("接收到远程发起断开本地连接请求,tarChannelHash:{}", tarChannelHash);
                break;
            }
            case 5: { //认证响应
                ConnectAuthRespPacks connectAuthRespPacks = (ConnectAuthRespPacks) baseTransferPacks;
                Byte state = connectAuthRespPacks.getState();
                String msg = connectAuthRespPacks.getMsg();
                if (state.byteValue() == ConnectRespPacks.STATE.SUCCESS) {
                    log.info("{}", msg);
                    //是否已开启心跳
                    Boolean isOpenHealth = channel.attr(Constants.HEALTH_IS_OPEN_KEY).get();
                    if(Objects.isNull(isOpenHealth)){
                        log.info("连接服务成功！{}", msg);
                        log.info("心跳已开启..");
                        channel.pipeline().addFirst(new IdleStateHandler(0, 30, Constants.DISCONNECT_HEALTH_SECONDS, TimeUnit.SECONDS));
                        channel.attr(Constants.HEALTH_IS_OPEN_KEY).set(Boolean.TRUE);
                    }

                    //连接成功后保存通道，并重置该连接的重试次数为0，只要连接上后都断连都将重试
                    if (Objects.isNull(Constants.REMOTE_CHANNEL)) {
                        Constants.REMOTE_CHANNEL = channel;
                    }
                    Client client = channel.attr(Constants.CLIENT_KEY).get();
                    Optional.ofNullable(client).ifPresent(cli -> client.getRecount().set(0));
                }else{
                    log.info("连接服务失败！{}", msg);
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
            case 9: { //心跳响应
                HealthRespPacks healthRespPacks = (HealthRespPacks) baseTransferPacks;
                Long msgId = healthRespPacks.getMsgId();
                log.info("接收到服务端的心跳响应，msgId:{}", msgId);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        IdleStateEvent event = (IdleStateEvent) evt;
        switch (event.state()) {
            case READER_IDLE: //读空闲
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
                // 不处理
                break;
        }
    }
}
