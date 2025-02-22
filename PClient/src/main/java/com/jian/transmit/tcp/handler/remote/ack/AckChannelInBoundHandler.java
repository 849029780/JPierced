package com.jian.transmit.tcp.handler.remote.ack;

import com.jian.beans.UdpSenderChannelInfo;
import com.jian.beans.transfer.*;
import com.jian.beans.transfer.beans.NetAddr;
import com.jian.beans.transfer.req.*;
import com.jian.beans.transfer.resp.ConnectAckChannelRespPacks;
import com.jian.beans.transfer.resp.ConnectRespPacks;
import com.jian.beans.transfer.resp.HealthRespPacks;
import com.jian.commons.Constants;
import com.jian.transmit.tcp.client.AbstractTcpClient;
import com.jian.transmit.tcp.client.TcpClient;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
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
                Long thisChannelHash = connectReqPacks.getSourceChannelHash();
                long tarChannelHash = connectReqPacks.getTarChannelHash();

                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                log.debug("连接本地服务:{}:{}，协议类型:{}", host, port, connectReqPacks.getProtocol());
                AbstractTcpClient tcpClient;
                if (connectReqPacks.getProtocol() == ConnectReqPacks.Protocol.HTTPS) {
                    tcpClient = TcpClient.getTcpClient().localHttpsChannelInitializer().option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                } else {
                    tcpClient = TcpClient.getTcpClient().localChannelInitializer().option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                }

                //发起连接本地
                tcpClient.connect(inetSocketAddress, (GenericFutureListener<ChannelFuture>) null).addListener(future -> {
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
                        log.info("连接本地服务:{}:{}成功！", host, port);
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
            case 9 -> { //心跳响应
                HealthRespPacks healthRespPacks = (HealthRespPacks) baseTransferPacks;
                Long msgId = healthRespPacks.getMsgId();
                log.debug("接收到服务端的ack心跳响应，msgId:{}", msgId);
            }
            case 11 -> { //消息处理
                MessageReqPacks messageReqPacks = (MessageReqPacks) baseTransferPacks;
                int msgLen = messageReqPacks.getMsgLen();
                String msg = messageReqPacks.getMsg();
                log.info("接收到服务端消息，消息长度：{}，内容:【{}】", msgLen, msg);
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
                    transimitChannel.pipeline().addFirst(new IdleStateHandler(Constants.DISCONNECT_HEALTH_SECONDS, Constants.HEART_DELAY, 0, TimeUnit.SECONDS));
                    channel.pipeline().addFirst(new IdleStateHandler(Constants.DISCONNECT_HEALTH_SECONDS, Constants.HEART_DELAY, 0, TimeUnit.SECONDS));
                    transimitChannel.attr(Constants.HEALTH_IS_OPEN_KEY).set(Boolean.TRUE);
                } else {
                    log.info("ack连接完成！{}", msg);
                }

                AbstractTcpClient abstractTcpClient = transimitChannel.attr(Constants.CLIENT_KEY).get();
                //连接成功后才允许断开重连
                Optional.ofNullable(abstractTcpClient).ifPresent(cli -> {
                    //可进行重连
                    abstractTcpClient.setCanReconnect(true);
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
            case 18 -> { //添加本地udp端口映射表
                UdpPortMappingAddReqPacks udpPortMappingAddReqPacks = (UdpPortMappingAddReqPacks) baseTransferPacks;
                List<NetAddr> netAddrList = udpPortMappingAddReqPacks.getNetAddrList();
                log.info("接收到来自服务端下发的udp端口映射到本地地址信息：{}..", netAddrList);
                for (NetAddr netAddr : netAddrList) {
                    Integer sourcePort = netAddr.getSourcePort();
                    String host = netAddr.getHost();
                    Integer port = netAddr.getPort();
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                    Constants.UDP_SERVER_MAPPING_ADDR.put(sourcePort, inetSocketAddress);
                }
            }
            case 19 -> { //移除本地udp端口映射
                UdpPortMappingRemReqPacks udpPortMappingRemReqPacks = (UdpPortMappingRemReqPacks) baseTransferPacks;
                Integer sourcePort = udpPortMappingRemReqPacks.getSourcePort();
                Constants.UDP_SERVER_MAPPING_ADDR.remove(sourcePort);
                List<Channel> channelList = Constants.UDP_SERVER_MAPPING_LOCAL_CHANNEL.remove(sourcePort);
                if (Objects.nonNull(channelList) && !channelList.isEmpty()) {
                    channelList.forEach(ch -> {
                        if(Objects.nonNull(ch)){
                            InetSocketAddress senderAddr = ch.attr(Constants.SENDER_ADDR).get();
                            String senderHost = senderAddr.getHostString();
                            int senderPort = senderAddr.getPort();
                            String sender = senderHost + ":" + senderPort;
                            Constants.SENDER_CHANNEL.remove(sender);
                        }
                        ch.close();
                    });
                }
            }
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE: //读空闲
                    log.warn("接收服务端响应的ack心跳超时！已自动认为该连接已断开，准备关闭本地连接...");
                    //这里close后会自动出发channelRemove事件，然后执行关闭本地相关端口连接
                    ctx.close();
                    break;
                case WRITER_IDLE: //写空闲
                    long msgId = System.nanoTime();
                    log.debug("发送ack心跳请求，msgId:{}", msgId);
                    HealthReqPacks healthReqPacks = new HealthReqPacks();
                    healthReqPacks.setMsgId(msgId);
                    ctx.writeAndFlush(healthReqPacks);
                    // 不处理
                    break;
                case ALL_IDLE: //读写空闲 不处理
                    break;
            }
        }
    }

}
