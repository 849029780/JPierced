package com.jian.transmit.handler.remote.transfer;

import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
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
    protected void channelRead0(ChannelHandlerContext ctx, BaseTransferPacks baseTransferPacks) {
        byte type = baseTransferPacks.getType();
        Channel channel = ctx.channel();
        Map<Long, Channel> localChannelMap = channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
        switch (type) {
            case 3 -> {//客户发起断开连接请求
                if (checkIsAuth(channel)) {
                    return;
                }
                DisConnectReqPacks disConnectReqPacks = (DisConnectReqPacks) baseTransferPacks;
                Long tarChannelHash = disConnectReqPacks.getTarChannelHash();
                Channel localChannel = localChannelMap.remove(tarChannelHash);
                //关闭本地连接的通道
                Optional.ofNullable(localChannel).ifPresent(localCh -> localCh.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE));
                log.debug("接收到远程发起断开本地连接请求,tarChannelHash:{}", tarChannelHash);
            }
            case 4 -> {//客户端认证请求
                ConnectAuthReqPacks connectAuthReqPacks = (ConnectAuthReqPacks) baseTransferPacks;
                Long key = connectAuthReqPacks.getKey();
                ClientInfo clientInfo = Constants.CLIENTS.get(key);

                //构建失败响应
                ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
                connectAuthRespPacks.setKey(key);

                //无权限
                if (Objects.isNull(clientInfo)) {
                    connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.FAIL);
                    connectAuthRespPacks.setMsg("当前key无权限连接！");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    log.warn("客户端连接key:{},当前key无权限连接！", key);
                    ctx.close();
                    return;
                }

                //当前客户端已在线
                if (clientInfo.isOnline()) {
                    connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.FAIL);
                    connectAuthRespPacks.setMsg("当前客户端已在线，不允许重复连接！");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    log.warn("客户端连接key:{},当前客户端已在线，不允许重复连接！", key);
                    ctx.close();
                    return;
                }
                String pwd = clientInfo.getPwd();
                //密码错误
                if (!pwd.equals(connectAuthReqPacks.getPwd())) {
                    connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.FAIL);
                    connectAuthRespPacks.setMsg("当前key密码错误！");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    log.warn("客户端连接key:{},密码:{}错误", key, pwd);
                    ctx.close();
                    return;
                }

                //登录成功，绑定属性关系
                clientInfo.setOnline(true);
                clientInfo.setRemoteChannel(channel);
                channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).set(new ConcurrentHashMap<>());
                channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).set(clientInfo);

                //发送认证响应
                connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.SUCCESS);
                connectAuthRespPacks.setAckPort(Constants.ACK_PORT);
                connectAuthRespPacks.setKey(clientInfo.getKey());
                connectAuthRespPacks.setMsg("客户端已完成认证！");
                clientInfo.getRemoteChannel().writeAndFlush(connectAuthRespPacks);

                String name = clientInfo.getName();
                log.info("客户端key:{},name:{}，已完成认证！如果ack连接未在{}秒内完成，则认证连接将被关闭！", key, name, Constants.ACK_AUTH_TIME_OUT);

                //认证完成后，若ack连接未在指定时间内完成，则需要踢出连接
                ScheduledFuture<?> schedule = ctx.executor().schedule(() -> {
                    log.info("客户端key:{},name:{}未在:{}秒内完成ack连接，该连接认证已被关闭！", key, name, Constants.ACK_AUTH_TIME_OUT);
                    channel.close();
                }, Constants.ACK_AUTH_TIME_OUT, TimeUnit.SECONDS);

                //设置认证超时器
                channel.attr(Constants.AUTH_SCHEDULED_KEY).set(schedule);

            }
            case 7 -> { //传输数据
                TransferDataPacks transferDataPacks = (TransferDataPacks) baseTransferPacks;
                Long targetChannelHash = transferDataPacks.getTargetChannelHash();
                Channel localChannel = localChannelMap.get(targetChannelHash);
                try {
                    localChannel.writeAndFlush(transferDataPacks.getDatas());
                } catch (NullPointerException e) {
                    ReferenceCountUtil.release(transferDataPacks.getDatas());
                }
            }
            case 8 -> { //接收心跳请求
                if (checkIsAuth(channel)) {
                    return;
                }
                ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
                HealthReqPacks healthReqPacks = (HealthReqPacks) baseTransferPacks;
                long msgId = healthReqPacks.getMsgId();
                log.info("接收到客户端key:{}，name:{}，的心跳请求，msgId:{}", clientInfo.getKey(), clientInfo.getName(), msgId);
                HealthRespPacks healthRespPacks = new HealthRespPacks();
                healthRespPacks.setMsgId(msgId);
                ctx.writeAndFlush(healthRespPacks);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    /***
     * 验证是否已经登录，未登录不允许发送任何数据
     */
    private boolean checkIsAuth(Channel channel) {
        boolean isAuth = false;
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        if (Objects.nonNull(clientInfo)) {
            isAuth = true;
        } else {
            channel.close();
        }
        return !isAuth;
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {
            Channel channel = ctx.channel();
            ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
            switch (event.state()) {
                case READER_IDLE:
                    //读空闲时，则认为客户端已失去连接
                    log.warn("客户端心跳接收已超过{}s，已默认为失去连接，准备关闭该客户端key:{},name:{}监听的所有端口...", Constants.DISCONNECT_HEALTH_SECONDS, clientInfo.getKey(), clientInfo.getName());
                    //这里close后会自动出发channelInactive事件，然后执行关闭本地相关端口连接
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    // 写空闲，不处理
                    break;
                case ALL_IDLE:
                    //所有空闲
                    break;
            }
        }
    }


}
