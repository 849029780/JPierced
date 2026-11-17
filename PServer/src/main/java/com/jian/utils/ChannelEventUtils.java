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

/***
 * channel和事件工具（一次性固化 Native 传输类型，DCL 保证 EventLoopGroup 全局单例并发安全）
 * @author Jian
 * @date 2024-09-14
 */
@Slf4j
public class ChannelEventUtils {

    public enum TransportType {
        IO_URING, EPOLL, KQUEUE, NIO
    }

    private static final TransportType DETECTED_TRANSPORT;
    private static final Class<? extends ServerSocketChannel> SERVER_SOCKET_CHANNEL_CLASS;
    private static final Class<? extends DatagramChannel> DATAGRAM_CHANNEL_CLASS;

    private static volatile EventLoopGroup BOSS_LOOP_GROUP;
    private static volatile EventLoopGroup WORK_LOOP_GROUP;

    static {
        if (IoUring.isAvailable()) {
            DETECTED_TRANSPORT = TransportType.IO_URING;
            SERVER_SOCKET_CHANNEL_CLASS = IoUringServerSocketChannel.class;
            DATAGRAM_CHANNEL_CLASS = IoUringDatagramChannel.class;
            log.info("检测到支持底层 Linux io_uring 高性能传输层");
        } else if (Epoll.isAvailable()) {
            DETECTED_TRANSPORT = TransportType.EPOLL;
            SERVER_SOCKET_CHANNEL_CLASS = EpollServerSocketChannel.class;
            DATAGRAM_CHANNEL_CLASS = EpollDatagramChannel.class;
            log.info("检测到支持底层 Linux epoll 传输层");
        } else if (KQueue.isAvailable()) {
            DETECTED_TRANSPORT = TransportType.KQUEUE;
            SERVER_SOCKET_CHANNEL_CLASS = KQueueServerSocketChannel.class;
            DATAGRAM_CHANNEL_CLASS = KQueueDatagramChannel.class;
            log.info("检测到支持底层 BSD/macOS kqueue 传输层");
        } else {
            DETECTED_TRANSPORT = TransportType.NIO;
            SERVER_SOCKET_CHANNEL_CLASS = NioServerSocketChannel.class;
            DATAGRAM_CHANNEL_CLASS = NioDatagramChannel.class;
            log.info("使用标准 Java NIO 传输层");
        }
    }

    /***
     * 根据预检测的底层传输层获取线程组
     */
    private static EventLoopGroup createEventLoopGroup(int threadNum) {
        IoHandlerFactory ioHandlerFactory = switch (DETECTED_TRANSPORT) {
            case IO_URING -> IoUringIoHandler.newFactory();
            case EPOLL -> EpollIoHandler.newFactory();
            case KQUEUE -> KQueueIoHandler.newFactory();
            case NIO -> NioIoHandler.newFactory();
        };
        return new MultiThreadIoEventLoopGroup(threadNum, ioHandlerFactory);
    }

    public static EventLoopGroup getBossLoopGroup() {
        if (BOSS_LOOP_GROUP == null) {
            synchronized (ChannelEventUtils.class) {
                if (BOSS_LOOP_GROUP == null) {
                    BOSS_LOOP_GROUP = createEventLoopGroup(Constants.BOSS_THREAD_NUM);
                }
            }
        }
        return BOSS_LOOP_GROUP;
    }

    public static EventLoopGroup getWorkLoopGroup() {
        if (WORK_LOOP_GROUP == null) {
            synchronized (ChannelEventUtils.class) {
                if (WORK_LOOP_GROUP == null) {
                    WORK_LOOP_GROUP = createEventLoopGroup(Constants.THREAD_NUM);
                }
            }
        }
        return WORK_LOOP_GROUP;
    }

    /***
     * 获取固化好的 ServerSocketChannel Class
     */
    public static Class<? extends ServerSocketChannel> getSocketChannelClass() {
        return SERVER_SOCKET_CHANNEL_CLASS;
    }

    /***
     * 获取固化好的 DatagramChannel Class
     */
    public static Class<? extends DatagramChannel> getDatagramChannelClass() {
        return DATAGRAM_CHANNEL_CLASS;
    }

}
