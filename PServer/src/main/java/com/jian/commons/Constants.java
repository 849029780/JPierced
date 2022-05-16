package com.jian.commons;

import com.auth0.jwt.algorithms.Algorithm;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.transmit.handler.local.LocalChannelInBoundHandler;
import com.jian.transmit.handler.local.LocalChannelInitializer;
import com.jian.transmit.handler.local.LocalHttpsChannelInitializer;
import com.jian.transmit.handler.local.LocalTcpChannelInBoundHandler;
import com.jian.transmit.handler.remote.RemoteChannelInitializer;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.Properties;
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
     * 服务端接受连接处理线程组
     */
    public static final EventLoopGroup BOSS_EVENT_LOOP_GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup() : KQueue.isAvailable() ? new KQueueEventLoopGroup() : new NioEventLoopGroup();
    /***
     * 服务端编解码工作线程组
     */
    public static final EventLoopGroup WORK_EVENT_LOOP_GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup() : KQueue.isAvailable() ? new KQueueEventLoopGroup() : new NioEventLoopGroup();

    /***
     * 服务端通道类型
     */
    public static final Class<? extends ServerSocketChannel> SERVER_SOCKET_CHANNEL_CLASS = Epoll.isAvailable() ? EpollServerSocketChannel.class : KQueue.isAvailable() ? KQueueServerSocketChannel.class : NioServerSocketChannel.class;

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
     * 将远程通道设置为不自动读的本地通道id，用于通道移除时使用，如果该本地通道移除时发现自己锁定了远程通道，则需要解锁，如果非当前本地通道锁定的远程通道，则不允许被解锁
     */
    public static final AttributeKey<ChannelId> LOCK_CHANNEL_ID_KEY = AttributeKey.valueOf("LOCK_CHANNEL_ID");

    /***
     * 绑定在远程通道上的 本地连接信息 map key为本地连接通道的hash，val为通道
     */
    public static final AttributeKey<Map<Long, Channel>> REMOTE_BIND_LOCAL_CHANNEL_KEY = AttributeKey.valueOf("REMOTE_BIND_LOCAL_CHANNEL");

    /***
     * 绑定在远程通道上的 当前客户端连接信息
     */
    public static final AttributeKey<ClientInfo> REMOTE_BIND_CLIENT_KEY = AttributeKey.valueOf("REMOTE_BIND_CLIENT");

    /***
     * 基础包头大小
     */
    public static final int BASE_PACK_SIZE = 4 + 1;

    /***
     * 本地服务端端口映射被穿透机器上的地址和端口(Client中的portMappingAddress)
     */
    //public static Map<Integer, ClientInfo> PORT_MAPPING_CLIENT = new ConcurrentHashMap<>();

    /***
     * 绑定在本地通道上的 当前客户端连接信息
     */
    public static final AttributeKey<ClientInfo> LOCAL_BIND_CLIENT_KEY = AttributeKey.valueOf("LOCAL_BIND_CLIENT");

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
    public static final ChannelInitializer LOCAL_CHANNEL_INITIALIZER = new LocalChannelInitializer();

    /***
     * 本地https通道初始化handler
     */
    public static final ChannelInitializer LOCAL_HTTPS_CHANNEL_INITIALIZER = new LocalHttpsChannelInitializer();


    /***
     * 远程通道初始化Handler
     */
    public static final ChannelInitializer REMOTE_CHANNEL_INITIALIZER = new RemoteChannelInitializer();

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
    public static Properties CONFIG = new Properties();

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
     * 传输端口配置文件属性名
     */
    public static final String TRANSMIT_PORT_PROPERTY = "transmit.port";
    /***
     * web端口配置文件属性名
     */
    public static final String WEB_PORT_PROPERTY = "web.port";

    /***
     * web登录的用户名
     */
    public static final String LOGIN_USERNAME_PROPERTY = "login.username";

    /***
     * web登录的密码
     */
    public static final String LOGIN_PWD_PROPERTY = "login.pwd";

    /***
     * 是否启动https
     */
    public static final String ENABLE_HTTPS_PROPERTY_NAME = "enable.https";

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
     * 默认传输端口
     */
    public static final String DEF_TRANSMIT_PORT = "9999";

    /***
     * 默认web端口
     */
    public static final String DEF_WEB_PORT = "8000";


}
