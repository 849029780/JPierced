package com.jian.commons;

import com.jian.beans.Client;
import io.netty.channel.Channel;
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
     * 基础包头大小
     */
    public static final int BASE_PACK_SIZE = 4 + 1;



}
