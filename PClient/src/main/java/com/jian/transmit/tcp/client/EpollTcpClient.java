package com.jian.transmit.tcp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;


/***
 * Epoll tcp client
 * @author Jian
 * @date 2025-01-21
 */
@Slf4j
public class EpollTcpClient extends AbstractTcpClient {

    /***
     * 所有系统都支持nio
     */
    @Override
    public boolean support() {
        return Epoll.isAvailable();
    }


    @Override
    public AbstractTcpClient getInstance() {
        if (AbstractTcpClient.EVENT_LOOP_GROUP == null) {
            synchronized (AbstractTcpClient.class) {
                if (AbstractTcpClient.EVENT_LOOP_GROUP == null) {
                    log.info("使用Epoll Channel.");
                    IoHandlerFactory ioHandlerFactory = EpollIoHandler.newFactory();
                    AbstractTcpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.THREAD_NUM, ioHandlerFactory);
                }
            }
        }
        EpollTcpClient epollTcpClient = new EpollTcpClient();
        epollTcpClient.bootstrap = new Bootstrap();
        epollTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        epollTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        epollTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        epollTcpClient.bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new io.netty.channel.WriteBufferWaterMark(128 * 1024, 512 * 1024));
        epollTcpClient.bootstrap.channel(EpollSocketChannel.class);
        epollTcpClient.bootstrap.group(AbstractTcpClient.EVENT_LOOP_GROUP);
        return epollTcpClient;
    }

}
