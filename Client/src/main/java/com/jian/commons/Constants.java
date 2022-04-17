package com.jian.commons;

import com.jian.handler.local.LocalChannelInitializer;
import com.jian.handler.remote.RemoteChannelInitializer;
import com.jian.start.Client;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.Properties;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class Constants {

    /***
     * 服务端编解码工作线程组
     */
    public static final EventLoopGroup WORK_EVENT_LOOP_GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup() : KQueue.isAvailable() ? new KQueueEventLoopGroup() : new NioEventLoopGroup();

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
     * 服务通道
     */
    public static Channel REMOTE_CHANNEL;

    /***
     * 基础包头大小
     */
    public static final int BASE_PACK_SIZE = 4 + 1;

    /***
     * 本地通道初始化handler
     */
    public static final ChannelInitializer LOCAL_CHANNEL_INITIALIZER = new LocalChannelInitializer();

    /***
     * 远程通道初始化Handler
     */
    public static final ChannelInitializer REMOTE_CHANNEL_INITIALIZER = new RemoteChannelInitializer();

    /***
     * 重连间隔时间 s
     */
    public static final int RECONNECT_DELAY = 30;

    /***
     * 重连最大次数
     */
    public static final int RECONNECT_MAX_COUNT = 5;

    /***
     * 超过该阈值未接收到心跳则代表该连接已断开
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
