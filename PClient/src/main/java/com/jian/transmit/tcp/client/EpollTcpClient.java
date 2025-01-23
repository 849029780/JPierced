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
        log.info("使用Epoll Channel.");
        if (Objects.isNull(AbstractTcpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = EpollIoHandler.newFactory();
            AbstractTcpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.THREAD_NUM, ioHandlerFactory);
        }
        EpollTcpClient epollTcpClient = new EpollTcpClient();
        epollTcpClient.bootstrap = new Bootstrap();
        epollTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        epollTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        epollTcpClient.bootstrap.channel(EpollSocketChannel.class);
        epollTcpClient.bootstrap.group(AbstractTcpClient.EVENT_LOOP_GROUP);
        //禁用组包
        //epollTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return epollTcpClient;
    }

}
