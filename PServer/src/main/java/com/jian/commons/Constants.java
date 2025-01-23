package com.jian.commons;

import com.auth0.jwt.algorithms.Algorithm;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.transmit.tcp.handler.local.LocalChannelInBoundHandler;
import com.jian.transmit.tcp.handler.local.LocalChannelInitializer;
import com.jian.transmit.tcp.handler.local.LocalHttpsChannelInitializer;
import com.jian.transmit.tcp.handler.local.LocalTcpChannelInBoundHandler;
import com.jian.transmit.tcp.handler.remote.CustomChannelInitializer;
import com.jian.transmit.tcp.handler.remote.ack.AckChannelInBoundHandler;
import com.jian.transmit.tcp.handler.remote.ack.AckMessageToMessageCodec;
import com.jian.transmit.tcp.handler.remote.transfer.RemoteChannelInBoundHandler;
import com.jian.transmit.tcp.handler.remote.transfer.RemoteMessageToMessageCodec;
import com.jian.transmit.udp.handler.LocalChannelInboundHandler;
import com.jian.transmit.udp.handler.LocalUdpChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class Constants {

    /***
     * 线程数
     */
    public static final int THREAD_NUM = 8;

    /***
     * boss线程数
     */
    public static final int BOSS_THREAD_NUM = 4;

    /***
     * 绑定在本地通道上的 当前通道hash
     */
    public static final AttributeKey<Long> THIS_CHANNEL_HASH_KEY = AttributeKey.valueOf("THIS_CHANNEL_HASH");

    /***
     * 绑定在本地通道上的 被穿透的通道hash
     */
    public static final AttributeKey<Long> TAR_CHANNEL_HASH_KEY = AttributeKey.valueOf("TAR_CHANNEL_HASH");

    /***
     * 绑定在本地通道上的 传输数据通道hash
     */
    public static final AttributeKey<Channel> REMOTE_CHANNEL_KEY = AttributeKey.valueOf("REMOTE_CHANNEL");
    /***
     * 绑定在数据传输通道上的ack通道属性
     */
    public static final AttributeKey<Channel> REMOTE_ACK_CHANNEL_KEY = AttributeKey.valueOf("REMOTE_ACK_CHANNEL");

    /***
     * 认证超时器属性
     */
    public static final AttributeKey<ScheduledFuture<?>> AUTH_SCHEDULED_KEY = AttributeKey.valueOf("AUTH_SCHEDULED");


    /***
     * 认证后未在该时间内进行ack连接，则踢出该认证及连接
     */
    public static final int ACK_AUTH_TIME_OUT = 10;

    /***
     * 基础包头大小
     */
    public static final int BASE_PACK_SIZE = 4 + 1;

    /***
     * 绑定在本地通道上的 当前客户端连接信息
     */
    public static final AttributeKey<ClientInfo> CLIENT_INFO_KEY = AttributeKey.valueOf("CLIENT_INFO");

    /***
     * 绑定在本地通道上 客户端方需要连接的地址信息
     */
    public static final AttributeKey<NetAddress> LOCAL_BIND_CLIENT_NET_LOCAL_KEY = AttributeKey.valueOf("LOCAL_BIND_CLIENT_NET_LOCAL");

    /***
     * 客户端配置信息
     */
    public static Map<Long, ClientInfo> CLIENTS = new ConcurrentHashMap<>();

    /***
     * 固定活动线程数的线程池
     */
    public static final ExecutorService FIXED_THREAD_POOL = Executors.newFixedThreadPool(3);

    /***
     * 读取本地端口的第一个handler
     */
    public static final LocalChannelInBoundHandler LOCAL_CHANNEL_IN_BOUND_HANDLER = new LocalChannelInBoundHandler();

    /***
     * 本地通道初始化handler
     */
    public static final ChannelInitializer<Channel> LOCAL_CHANNEL_INITIALIZER = new LocalChannelInitializer();

    /***
     * 本地https通道初始化handler
     */
    public static final ChannelInitializer<Channel> LOCAL_HTTPS_CHANNEL_INITIALIZER = new LocalHttpsChannelInitializer();


    /***
     * 远程通道初始化Handler
     */
    public static final ChannelInitializer<Channel> REMOTE_CHANNEL_INITIALIZER = new CustomChannelInitializer(new RemoteMessageToMessageCodec(), new RemoteChannelInBoundHandler());

    /***
     * ack端口
     */
    public static int ACK_PORT;

    /***
     * 远程通道ack初始化handler
     */
    public static final ChannelInitializer<Channel> REMOTE_ACK_CHANNEL_INITIALIZER = new CustomChannelInitializer(new AckMessageToMessageCodec(), new AckChannelInBoundHandler());

    /***
     *
     */
    public static final LocalTcpChannelInBoundHandler LOCAL_TCP_CHANNEL_IN_BOUND_HANDLER = new LocalTcpChannelInBoundHandler();

    /***
     * 通道心跳间隔阈值 秒s，超过该时间的通道未发送心跳，则默认该通道已失去连接，需要关闭通道并停止本地端口监听
     */
    public static final Integer DISCONNECT_HEALTH_SECONDS = 70;

    /***
     * 配置属性
     */
    public static ServerConfig CONFIG;

    /***
     * vertx
     */
    public static Vertx VERTX;

    /***
     * vertx部署的web实例id 用于重启web
     */
    public static String VERTX_WEB_DEPLOY_ID;

    /***
     * 传输服务监听的端口channel 用于重启操作
     */
    public static Channel LISTEN_REMOTE_CHANNEL;
    /***
     * 是否启用Https
     */
    public static boolean IS_ENABLE_HTTPS = false;

    /***
     * ssl server
     */
    public static SslContext SSL_LOCAL_PORT_CONTEXT;

    /***
     * 数据传输ssl
     */
    public static SslContext SSL_TRANSMIT_PORT_CONTEXT;

    /***
     * JWT校验器
     */
    public static final Algorithm JWT_ALGORITHM = Algorithm.HMAC512("jian0321");

    /***
     * udp handler初始化器
     */
    public static final ChannelInitializer<DatagramChannel> LOCAL_UDP_CHANNEL_INITIALIZER = new LocalUdpChannelInitializer(new LocalChannelInboundHandler());


}
