package com.jian.transmit.handler.remote.ack;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.transmit.Server;
import com.jian.utils.JsonUtils;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/***
 * ack通道消息处理
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
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        switch (type) {
            case 2 -> {//连接响应
                ConnectRespPacks connectRespPacks = (ConnectRespPacks) baseTransferPacks;
                Channel remoteChannel = clientInfo.getRemoteChannel();
                Map<Long, Channel> localChannelMap = remoteChannel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
                Channel localChannel = localChannelMap.get(connectRespPacks.getThisChannelHash());
                if (Objects.isNull(localChannel)) {
                    log.warn("连接响应本地连接通道已不存在！已告知被穿透的客户端断开已连接的服务！");
                    //本地通道不存在时，则需要关闭被穿透机器连接的客户端
                    DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                    disConnectReqPacks.setTarChannelHash(connectRespPacks.getTarChannelHash());
                    remoteChannel.writeAndFlush(disConnectReqPacks);
                    return;
                }
                //连接成功
                if (connectRespPacks.getState() == ConnectRespPacks.STATE.SUCCESS) {
                    //设置本地通道为自动读
                    localChannel.config().setAutoRead(Boolean.TRUE);
                } else {
                    //远程连接失败，则关闭本地的连接
                    localChannel.close();
                }
            }
            case 8 -> { //接收心跳请求
                HealthReqPacks healthReqPacks = (HealthReqPacks) baseTransferPacks;
                long msgId = healthReqPacks.getMsgId();
                log.debug("接收到客户端key:{}，name:{}，的ack心跳请求，msgId:{}", clientInfo.getKey(), clientInfo.getName(), msgId);
                HealthRespPacks healthRespPacks = new HealthRespPacks();
                healthRespPacks.setMsgId(msgId);
                ctx.writeAndFlush(healthRespPacks);
            }
            case 11 -> { //消息
                MessageReqPacks messageReqPacks = (MessageReqPacks) baseTransferPacks;
                int msgLen = messageReqPacks.getMsgLen();
                String msg = messageReqPacks.getMsg();
                log.info("接收到客户端key:{}，name:{}的消息，消息长度：{}，内容:【{}】", clientInfo.getKey(), clientInfo.getName(), msgLen, msg);
            }
            case 12 -> { //ack通道连接
                ConnectAckChannelReqPacks connectAckChannelReqPacks = (ConnectAckChannelReqPacks) baseTransferPacks;
                ConnectAckChannelRespPacks connectAckChannelRespPacks = new ConnectAckChannelRespPacks();
                long key = connectAckChannelReqPacks.getKey();
                //key为空，ack连接失败
                if (key == 0) {
                    connectAckChannelRespPacks.setState(BaseTransferPacks.STATE.FAIL);
                    connectAckChannelRespPacks.setMsg("该客户端key为0！ack通道连接失败！");
                    //发送完成后关闭通道
                    ctx.writeAndFlush(connectAckChannelRespPacks).addListener(ChannelFutureListener.CLOSE);
                    return;
                }

                clientInfo = Constants.CLIENTS.get(key);
                Channel remoteChannel;
                //如果为空或不在线，则活动通道连接失败
                if (Objects.isNull(clientInfo) || Objects.isNull(remoteChannel = clientInfo.getRemoteChannel()) || !clientInfo.isOnline()) {
                    connectAckChannelRespPacks.setState(BaseTransferPacks.STATE.FAIL);
                    connectAckChannelRespPacks.setMsg("该客户端不在线！或该客户端不存在！ack通道连接失败！");
                    //发送完成后关闭通道
                    ctx.writeAndFlush(connectAckChannelRespPacks).addListener(ChannelFutureListener.CLOSE);
                    return;
                }

                //认证超时器
                ScheduledFuture<?> scheduledFuture = remoteChannel.attr(Constants.AUTH_SCHEDULED_KEY).get();
                if (Objects.nonNull(scheduledFuture)) {
                    if (scheduledFuture.isDone()) {
                        connectAckChannelRespPacks.setState(BaseTransferPacks.STATE.FAIL);
                        connectAckChannelRespPacks.setMsg("该客户端ack连接超时！已被关闭连接");
                        //发送完成后关闭通道
                        ctx.writeAndFlush(connectAckChannelRespPacks).addListener(ChannelFutureListener.CLOSE);
                        return;
                    }
                    //取消该认证定时器
                    scheduledFuture.cancel(true);
                }

                //设置ack通道到客户信息中
                clientInfo.setAckChannel(channel);
                //ack通道上也绑定客户端信息
                channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).set(clientInfo);

                //---------------------传输通道后续设置------------------
                Map<Integer, NetAddress> portMappingAddress = clientInfo.getPortMappingAddress();
                Set<Integer> ports = portMappingAddress.keySet();

                log.info("客户端key:{}，name:{}，ack已连接，已开启心跳检测！", clientInfo.getKey(), clientInfo.getName());
                //开启心跳检测
                remoteChannel.pipeline().addFirst(new IdleStateHandler(0, 0, Constants.DISCONNECT_HEALTH_SECONDS, TimeUnit.SECONDS));

                MessageReqPacks messageReqPacks = new MessageReqPacks();
                if (ports.size() == 0) {
                    messageReqPacks.setMsg("客户端暂未配置映射端口！");
                    channel.writeAndFlush(messageReqPacks);
                } else {
                    messageReqPacks.setMsg("客户端配置的服务端映射端口为：" + JsonUtils.toJson(ports));
                    channel.writeAndFlush(messageReqPacks);
                    //异步启动该客户端需要监听的端口
                    Server.listenLocal(ports, clientInfo);
                }
                //---------------------传输通道后续设置------------------

                connectAckChannelRespPacks.setState(BaseTransferPacks.STATE.SUCCESS);
                connectAckChannelRespPacks.setMsg("ack绑定在客户端通道上成功！");
                ctx.writeAndFlush(connectAckChannelRespPacks);

            }
            case 14 -> { //ack通道消息-设置是否自动读
                AutoreadReqPacks autoreadReqPacks = (AutoreadReqPacks) baseTransferPacks;
                //从ack通道上获取传输通道
                Channel remoteChannel = clientInfo.getRemoteChannel();
                if (Objects.nonNull(remoteChannel)) {
                    Map<Long, Channel> localChannelMap = remoteChannel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
                    Channel localChannel = localChannelMap.get(autoreadReqPacks.getTarChannelHash());
                    Optional.ofNullable(localChannel).ifPresent(ch -> ch.config().setAutoRead(autoreadReqPacks.isAutoRead()));
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }

}
