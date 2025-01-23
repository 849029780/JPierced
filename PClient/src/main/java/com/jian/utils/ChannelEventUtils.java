package com.jian.utils;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import lombok.extern.slf4j.Slf4j;


/***
 * channel和事件工具
 * @author Jian
 * @date 2024-09-14
 */
@Slf4j
public class ChannelEventUtils {

    /***
     * 根据操作系统获取对应datagramChannel
     * @return EventLoopGroup
     */
    public static Class<? extends DatagramChannel> getDatagramChannelClass() {
        Class<? extends DatagramChannel> clazz;
        if (IoUring.isAvailable()) {
            clazz = IoUringDatagramChannel.class;
        } else if (Epoll.isAvailable()) {
            clazz = EpollDatagramChannel.class;
        } else if (KQueue.isAvailable()) {
            clazz = KQueueDatagramChannel.class;
        } else {
            clazz = NioDatagramChannel.class;
        }
        return clazz;
    }


}
