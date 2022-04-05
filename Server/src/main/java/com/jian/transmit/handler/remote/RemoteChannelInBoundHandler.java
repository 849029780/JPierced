package com.jian.transmit.handler.remote;

import com.jian.transmit.ClientInfo;
import com.jian.beans.transfer.*;
import com.jian.commons.Constants;
import com.jian.transmit.NetAddress;
import com.jian.transmit.Server;
import com.jian.utils.JsonUtils;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
                log.debug("接收到远程发起断开本地连接请求,tarChannelHash:{}", tarChannelHash);
                break;
            }
            case 4: {//客户端认证请求
                ConnectAuthReqPacks connectAuthReqPacks = (ConnectAuthReqPacks) baseTransferPacks;
                Long key = connectAuthReqPacks.getKey();
                ClientInfo clientInfo = Constants.CLIENTS.get(key);

                //构建失败响应
                ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
                connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.FAIL);
                connectAuthRespPacks.setKey(key);

                //无权限
                if (Objects.isNull(clientInfo)) {
                    connectAuthRespPacks.setMsg("当前key无权限连接！");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    log.info("客户端连接key:{},当前key无权限连接！", key);
                    return;
                }

                //当前客户端已在线
                if (clientInfo.isOnline()) {
                    connectAuthRespPacks.setMsg("当前客户端已在线，不允许重复连接！");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    log.info("客户端连接key:{},当前客户端已在线，不允许重复连接！", key);
                    return;
                }
                String pwd = clientInfo.getPwd();
                //密码错误
                if (!pwd.equals(connectAuthReqPacks.getPwd())) {
                    connectAuthRespPacks.setMsg("当前key密码错误！");
                    ctx.writeAndFlush(connectAuthRespPacks).addListener(ChannelFutureListener.CLOSE);
                    log.info("客户端连接key:{},密码:{}错误", key, pwd);
                    return;
                }

                //登录成功，绑定属性关系
                clientInfo.setOnline(true);
                clientInfo.setRemoteChannel(channel);
                channel.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).set(new ConcurrentHashMap<>());
                channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).set(clientInfo);


                Map<Integer, NetAddress> portMappingAddress = clientInfo.getPortMappingAddress();
                Set<Integer> ports = portMappingAddress.keySet();

                //开启心跳检测
                channel.pipeline().addFirst(new IdleStateHandler(0,0, Constants.DISCONNECT_HEALTH_SECONDS, TimeUnit.SECONDS));

                connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.SUCCESS);
                connectAuthRespPacks.setKey(clientInfo.getKey());
                log.info("客户端key:{},name:{}已连接！", clientInfo.getKey(), clientInfo.getName());
                if(ports.size() == 0){
                    connectAuthRespPacks.setMsg("客户端暂未配置映射端口！");
                }else{
                    connectAuthRespPacks.setMsg("客户端配置的服务端映射端口为：" + JsonUtils.toJson(ports));
                }
                clientInfo.getRemoteChannel().writeAndFlush(connectAuthRespPacks);

                //异步启动该客户端需要监听的端口
                Server.listenLocal(ports, clientInfo);
                break;
            }
            case 7: { //传输数据
                TransferDataPacks transferDataPacks = (TransferDataPacks) baseTransferPacks;
                Long targetChannelHash = transferDataPacks.getTargetChannelHash();
                Channel localChannel = localChannelMap.get(targetChannelHash);
                Optional.ofNullable(localChannel).ifPresent(ch -> ch.writeAndFlush(transferDataPacks.getDatas()));
                break;
            }
            case 8: { //接收心跳请求
                ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
                HealthReqPacks healthReqPacks = (HealthReqPacks) baseTransferPacks;
                Long msgId = healthReqPacks.getMsgId();
                log.info("接收到客户端key:{}，name:{}，的心跳请求，msgId:{}", clientInfo.getKey(), clientInfo.getName(), msgId);
                HealthRespPacks healthRespPacks = new HealthRespPacks();
                healthRespPacks.setMsgId(msgId);
                ctx.writeAndFlush(healthRespPacks);
            }
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = channel.attr(Constants.REMOTE_BIND_CLIENT_KEY).get();
        IdleStateEvent event = (IdleStateEvent) evt;
        switch (event.state()) {
            case READER_IDLE:
                break;
            case WRITER_IDLE:
                // 写空闲，不处理
                break;
            case ALL_IDLE:
                //所有空闲
                log.warn("客户端心跳接收已超过{}s，已默认为失去连接，准备关闭该客户端key:{},name:{}监听的所有端口...", Constants.DISCONNECT_HEALTH_SECONDS, clientInfo.getKey(), clientInfo.getName());
                //这里close后会自动出发channelRemove事件，然后执行关闭本地相关端口连接
                ctx.close();
                break;
        }
    }


}
