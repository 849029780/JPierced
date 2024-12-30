package com.jian.utils;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringSocketChannel;


/***
 * channel和事件工具
 * @author Jian
 * @date 2024-09-14
 */
public class ChannelEventUtils {

    /***
     * 根据操作系统获取线程组
     * @param threadNum 线程数量
     * @return EventLoopGroup
     */
    public static EventLoopGroup getEventLoopGroup(int threadNum) {
        IoHandlerFactory ioHandlerFactory;
        if (IoUring.isAvailable()) {
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


    /***
     * 根据操作系统获取对应seocketChannel
     * @return EventLoopGroup
     */
    public static Class<? extends SocketChannel> getSocketChannelClass() {
        Class<? extends SocketChannel> clazz;
        if (IoUring.isAvailable()) {
            clazz = IoUringSocketChannel.class;
        } else if (Epoll.isAvailable()) {
            clazz = EpollSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            clazz = KQueueSocketChannel.class;
        } else {
            clazz = NioSocketChannel.class;
        }
        return clazz;
    }


}
