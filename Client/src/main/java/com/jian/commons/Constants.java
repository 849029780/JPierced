package com.jian.commons;

import com.jian.handler.local.LocalChannelInitializer;
import com.jian.handler.remote.RemoteChannelInitializer;
import com.jian.start.Client;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class Constants {

    /***
     * 服务端编解码工作线程组
     */
    public static final EventLoopGroup WORK_EVENT_LOOP_GROUP = new NioEventLoopGroup();

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
    public static Map<Long, Channel> LOCAL_CHANNEL_MAP = new ConcurrentHashMap<>();

    /***
     * 通道上绑定的连接信息
     */
    public static final AttributeKey<Client> CLIENT_KEY = AttributeKey.valueOf("CLIENT_KEY");

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
    public static final int RECONNECT_DELAY = 5;

    /***
     * 重连最大次数
     */
    public static final int RECONNECT_MAX_COUNT = 3;

}
