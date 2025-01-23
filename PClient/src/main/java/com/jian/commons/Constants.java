package com.jian.commons;

import com.jian.beans.UdpSenderChannelInfo;
import com.jian.transmit.tcp.client.AbstractTcpClient;
import com.jian.transmit.tcp.handler.local.LocalChannelInBoundHandler;
import com.jian.transmit.tcp.handler.local.LocalChannelInitializer;
import com.jian.transmit.tcp.handler.local.LocalHttpsChannelInitializer;
import com.jian.transmit.tcp.handler.remote.ack.AckChannelInitializer;
import com.jian.transmit.tcp.handler.remote.transfer.RemoteChannelInitializer;
import com.jian.transmit.udp.handler.LocalUdpChannelInboundHandler;
import com.jian.transmit.udp.handler.LocalUdpChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;

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

    public static final int UDP_THREAD_NUM = 4;

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
    public static final AttributeKey<AbstractTcpClient> CLIENT_KEY = AttributeKey.valueOf("CLIENT_KEY");

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
     * 配置
     */
    public static ClientConfig CONFIG;


    /***
     * 发送者地址信息
     */
    public static final AttributeKey<InetSocketAddress> SENDER_ADDR = AttributeKey.valueOf("SENDER_ADDR");

    /***
     * 服务端接收udp数据的端口
     */
    public static final AttributeKey<Integer> SOURCE_PORT = AttributeKey.valueOf("SOURCE_PORT");

    /***
     * udp消息处理器
     */
    public static final LocalUdpChannelInboundHandler LOCAL_UDP_CHANEL_INBOUND_HANDLER = new LocalUdpChannelInboundHandler();

    /***
     * udp通道初始化器
     */
    public static final ChannelInitializer<DatagramChannel> UDP_CHANNEL_INITIALIZER = new LocalUdpChannelInitializer();

    /***
     * 服务端udp端口号对应本地udp端口和地址
     */
    public static Map<Integer, InetSocketAddress> UDP_SERVER_MAPPING_ADDR = new ConcurrentHashMap<>();

    /***
     * 发送者映射本地的udp channel
     */
    public static Map<String, UdpSenderChannelInfo> SENDER_CHANNEL = new ConcurrentHashMap<>();


}
