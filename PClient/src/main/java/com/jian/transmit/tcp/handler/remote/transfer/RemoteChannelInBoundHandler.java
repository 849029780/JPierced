package com.jian.transmit.tcp.handler.remote.transfer;

import com.jian.beans.UdpSenderChannelInfo;
import com.jian.beans.transfer.*;
import com.jian.beans.transfer.beans.NetAddr;
import com.jian.beans.transfer.req.*;
import com.jian.beans.transfer.resp.ConnectAuthRespPacks;
import com.jian.beans.transfer.resp.ConnectRespPacks;
import com.jian.beans.transfer.resp.HealthRespPacks;
import com.jian.commons.Constants;
import com.jian.transmit.tcp.client.AbstractTcpClient;
import com.jian.transmit.tcp.client.TcpClient;
import com.jian.transmit.udp.client.UdpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.*;
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
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(remoteAddress.getAddress(), connectAuthRespPacks.getAckPort());
                    TcpClient.getTcpClient().remoteAckInitializer().connect(inetSocketAddress, (Consumer<ChannelFuture>) future -> {
                        Channel ackChannel = future.channel();
                        if (future.isSuccess()) {
                            String keyStr = Constants.CONFIG.getServer().getUsername();
                            ConnectAckChannelReqPacks connectAckChannelReqPacks = new ConnectAckChannelReqPacks();
                            connectAckChannelReqPacks.setKey(Long.parseLong(keyStr));
                            ackChannel.writeAndFlush(connectAckChannelReqPacks);
                        } else {
                            log.error("ack通道连接失败！");
                            ackChannel.close();
                            channel.close();
                        }
                    });
                } else {
                    log.warn("连接服务认证失败！{}", msg);
                    channel.close();
                }
            }
            case 7 -> { //传输数据
                TcpTransferDataPacks tcpTransferDataPacks = ((TcpTransferDataPacks) baseTransferPacks);
                Long targetChannelHash = tcpTransferDataPacks.getTarChannelHash();
                Channel localChannel = Constants.LOCAL_CHANNEL_MAP.get(targetChannelHash);
                ByteBuf datas = tcpTransferDataPacks.getDatas();
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
                log.info("接收到服务端要求断开和服务端的连接！");
                AbstractTcpClient abstractTcpClient = channel.attr(Constants.CLIENT_KEY).get();
                //连接成功后才会将重试次数置空
                Optional.ofNullable(abstractTcpClient).ifPresent(cli -> {
                    //设置不可进行重连
                    abstractTcpClient.setCanReconnect(false);
                });
                channel.close();
            }
            case 20 -> { //udp数据传输
                UdpTransferDataPacks udpTransferDataPacks = ((UdpTransferDataPacks) baseTransferPacks);
                Integer sourcePort = udpTransferDataPacks.getSourcePort();
                ByteBuf datas = udpTransferDataPacks.getDatas();
                //目标地址
                InetSocketAddress tarInetAddr = Constants.UDP_SERVER_MAPPING_ADDR.get(sourcePort);
                String senderHost = udpTransferDataPacks.getSenderHost();
                Integer senderPort = udpTransferDataPacks.getSenderPort();
                String sender = senderHost + ":" + senderPort;
                UdpSenderChannelInfo udpSenderChannelInfo = Constants.SENDER_CHANNEL.get(sender);
                if (Objects.isNull(udpSenderChannelInfo)) {
                    udpSenderChannelInfo = new UdpSenderChannelInfo();
                    Constants.SENDER_CHANNEL.put(sender, udpSenderChannelInfo);
                    InetSocketAddress senderAddr = new InetSocketAddress(udpTransferDataPacks.getSenderHost(), udpTransferDataPacks.getSenderPort());
                    Channel udpChannel = UdpClient.getUdpClient().init().bind(0, sourcePort, senderAddr);
                    udpSenderChannelInfo.setChannel(udpChannel);
                    List<Channel> channels = Constants.UDP_SERVER_MAPPING_LOCAL_CHANNEL.get(sourcePort);
                    if (Objects.isNull(channels)) {
                        channels = Collections.synchronizedList(new ArrayList<>());
                        Constants.UDP_SERVER_MAPPING_LOCAL_CHANNEL.put(sourcePort, channels);
                    }
                    channels.add(udpChannel);
                }
                Channel localUdpChannel = udpSenderChannelInfo.getChannel();
                DatagramPacket datagramPacket = new DatagramPacket(datas, tarInetAddr);
                udpSenderChannelInfo.setTime(LocalDateTime.now());
                localUdpChannel.writeAndFlush(datagramPacket);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE: //读空闲
                    log.warn("接收服务端响应的心跳超时！已自动认为该连接已断开，准备关闭本地连接...");
                    //这里close后会自动出发channelRemove事件，然后执行关闭本地相关端口连接
                    ctx.close();
                    break;
                case WRITER_IDLE: //写空闲
                    long msgId = System.nanoTime();
                    log.info("发送心跳请求，msgId:{}", msgId);
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
