package com.jian.commons;

import com.jian.handler.local.LocalChannelInBoundHandler;
import com.jian.handler.local.LocalChannelInitializer;
import com.jian.handler.local.LocalHttpsChannelInitializer;
import com.jian.handler.remote.ack.AckChannelInitializer;
import com.jian.handler.remote.transfer.RemoteChannelInitializer;
import com.jian.start.Client;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class Constants {

    /***
     * 线程数
     */
    public static final int THREAD_NUM = 16;
    /***
     * 编解码工作线程组
     */
    public static final EventLoopGroup WORK_EVENT_LOOP_GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup(THREAD_NUM) : KQueue.isAvailable() ? new KQueueEventLoopGroup(THREAD_NUM) : new NioEventLoopGroup(THREAD_NUM);

    /***
     * ack通道工作线程，单独管理，防止传输工作线程导致ack通道数据发送延迟
     */
    public static final int ACK_WORK_THREAD_NUM = 4;

    /***
     * ack通道工作线程
     */
    public static final EventLoopGroup ACK_WORK_EVENT_LOOP_GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup(ACK_WORK_THREAD_NUM) : KQueue.isAvailable() ? new KQueueEventLoopGroup(ACK_WORK_THREAD_NUM) : new NioEventLoopGroup(ACK_WORK_THREAD_NUM);

    /***
     * 通道类型
     */
    public static final Class<? extends SocketChannel> SOCKET_CHANNEL_CLASS = Epoll.isAvailable() ? EpollSocketChannel.class : KQueue.isAvailable() ? KQueueSocketChannel.class : NioSocketChannel.class;

    /***
     * 绑定在本地通道上的 当前通道hash
     */
    public static final AttributeKey<Long> THIS_CHANNEL_HASH_KEY = AttributeKey.valueOf("THIS_CHANNEL_HASH");

    /***
     * 绑定在本地通道上的源端 通道hash
     */
    public static final AttributeKey<Long> TAR_CHANNEL_HASH_KEY = AttributeKey.valueOf("TAR_CHANNEL_HASH");

    /***
     * 本地连接的所有通道
     */
    public static Map<Long, Channel> LOCAL_CHANNEL_MAP;

    /***
     * 通道上绑定的连接信息
     */
    public static final AttributeKey<Client> CLIENT_KEY = AttributeKey.valueOf("CLIENT_KEY");

    /***
     * 远程通道是否已开启心跳
     */
    public static final AttributeKey<Boolean> HEALTH_IS_OPEN_KEY = AttributeKey.valueOf("HEALTH_IS_OPEN");

    /***
     * 和远程的数据传输通道
     */
    public static Channel REMOTE_TRANSIMIT_CHANNEL;

    /***
     * 和远程的ack通道
     */
    public static Channel REMOTE_ACK_CHANNEL;

    /***
     * 基础包头大小
     */
    public static final int BASE_PACK_SIZE = 4 + 1;

    /***
     * 本地通道初始化handler
     */
    public static final ChannelInitializer<Channel> LOCAL_CHANNEL_INITIALIZER = new LocalChannelInitializer();

    /***
     * 本地通道https初始化器
     */
    public static final ChannelInitializer<Channel> LOCAL_HTTPS_CHANNEL_INITIALIZER = new LocalHttpsChannelInitializer();

    /***
     * 本地通道第一个处理器
     */
    public static LocalChannelInBoundHandler LOCAL_CHANNEL_IN_BOUND_HANDLER = new LocalChannelInBoundHandler();

    /***
     * 远程通道初始化Handler
     */
    public static final ChannelInitializer<Channel> REMOTE_CHANNEL_INITIALIZER = new RemoteChannelInitializer();

    /***
     * 远程ack通道初始化handler
     */
    public static final ChannelInitializer<Channel> REMOTE_ACK_CHANNEL_INITIALIZER = new AckChannelInitializer();

    /***
     * 本地ssl context
     */
    public static SslContext LOCAL_SSL_CONTEXT;

    /***
     * 远程ssl context
     */
    public static SslContext REMOTE_SSL_CONTEXT;

    /***
     * 重连间隔时间 s
     */
    public static final int RECONNECT_DELAY = 30;

    /***
     * 心跳间隔时间 s
     */
    public static final int HEART_DELAY = 30;

    /***
     * 重连缓存线程
     */
    public static final ExecutorService CACHED_EXECUTOR_POOL = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

    /***
     * 超过该阈值未接收到心跳则代表该连接已断开 s
     */
    public static final int DISCONNECT_HEALTH_SECONDS = 70;

    /***
     * 服务地址属性名
     */
    public static final String HOST_PROPERTY_NAME = "server.host";

    /***
     * 服务端口属性名
     */
    public static final String PORT_PROPERTY_NAME = "server.port";

    /***
     * key属性名
     */
    public static final String KEY_PROPERTY_NAME = "key";

    /***
     * pwd属性名
     */
    public static final String PWD_PROPERTY_NAME = "pwd";

    /***
     * 配置
     */
    public static Properties CONFIG = new Properties();


}
