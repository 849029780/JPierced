package com.jian.utils;

import com.jian.commons.Constants;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;


/***
 * channel和事件工具
 * @author Jian
 * @date 2024-09-14
 */
@Slf4j
public class ChannelEventUtils {

    private static EventLoopGroup BOSS_LOOP_GROUP;

    private static EventLoopGroup WORK_LOOP_GROUP;

    /***
     * 根据操作系统获取线程组
     * @param threadNum 线程数量
     * @return EventLoopGroup
     */
    private static EventLoopGroup getEventLoopGroup(int threadNum) {
        IoHandlerFactory ioHandlerFactory;
        //if (IoUring.isAvailable()) {
        if (false) {
            ioHandlerFactory = IoUringIoHandler.newFactory();
        } else if (Epoll.isAvailable()) {
            ioHandlerFactory = EpollIoHandler.newFactory();
        } else if (KQueue.isAvailable()) {
            ioHandlerFactory = KQueueIoHandler.newFactory();
        } else {
            ioHandlerFactory = NioIoHandler.newFactory();
        }
        return new MultiThreadIoEventLoopGroup(threadNum, ioHandlerFactory);
    }

    public static EventLoopGroup getBossLoopGroup() {
        if (Objects.isNull(BOSS_LOOP_GROUP)) {
            BOSS_LOOP_GROUP = getEventLoopGroup(Constants.BOSS_THREAD_NUM);
        }
        return BOSS_LOOP_GROUP;
    }


    public static EventLoopGroup getWorkLoopGroup() {
        if (Objects.isNull(WORK_LOOP_GROUP)) {
            WORK_LOOP_GROUP = getEventLoopGroup(Constants.THREAD_NUM);
        }
        return WORK_LOOP_GROUP;
    }


    /***
     * 根据操作系统获取对应seocketChannel
     * @return EventLoopGroup
     */
    public static Class<? extends ServerSocketChannel> getSocketChannelClass() {
        Class<? extends ServerSocketChannel> clazz;
        //if (IoUring.isAvailable()) {
        if (false) {
            clazz = IoUringServerSocketChannel.class;
            log.info("使用IO_Uring Channel.");
        } else if (Epoll.isAvailable()) {
            clazz = EpollServerSocketChannel.class;
            log.info("使用Epoll Channel.");
        } else if (KQueue.isAvailable()) {
            clazz = KQueueServerSocketChannel.class;
            log.info("使用Kqueue Channel.");
        } else {
            clazz = NioServerSocketChannel.class;
            log.info("使用Nio Channel.");
        }
        return clazz;
    }


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
