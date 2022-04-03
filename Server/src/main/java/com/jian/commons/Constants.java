package com.jian.commons;

import com.jian.start.ClientConnectInfo;
import com.jian.handler.local.LocalChannelInitializer;
import com.jian.handler.remote.RemoteChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.AttributeKey;

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
     * 服务端接受连接处理线程组
     */
    public static final EventLoopGroup BOSS_EVENT_LOOP_GROUP = new NioEventLoopGroup();
    /***
     * 服务端编解码工作线程组
     */
    public static final EventLoopGroup WORK_EVENT_LOOP_GROUP = new NioEventLoopGroup();

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
     * 绑定在远程通道上的 本地连接信息 map key为本地连接通道的hash，val为通道
     */
    public static final AttributeKey<Map<Long, Channel>> REMOTE_BIND_LOCAL_CHANNEL_KEY = AttributeKey.valueOf("REMOTE_BIND_LOCAL_CHANNEL");

    /***
     * 绑定在远程通道上的 当前客户端连接信息
     */
    public static final AttributeKey<ClientConnectInfo> REMOTE_BIND_CLIENT_KEY = AttributeKey.valueOf("REMOTE_BIND_CLIENT");

    /***
     * 基础包头大小
     */
    public static final int BASE_PACK_SIZE = 4 + 1;

    /***
     * 本地服务端端口映射被穿透机器上的地址和端口(Client中的portMappingAddress)
     */
    public static Map<Integer, ClientConnectInfo> PORT_MAPPING_CLIENT = new ConcurrentHashMap<>();

    /***
     * 客户端配置信息
     */
    public static Map<Long, ClientConnectInfo> CLIENTS = new ConcurrentHashMap<>();

    /***
     * 固定活动线程数的线程池
     */
    public static final ExecutorService FIXED_THREAD_POOL = Executors.newFixedThreadPool(5);

    public static final ChannelInitializer LOCAL_CHANNEL_INITIALIZER = new LocalChannelInitializer();

    public static final ChannelInitializer REMOTE_CHANNEL_INITIALIZER = new RemoteChannelInitializer();


}
