package com.jian.transmit.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;


/***
 * Epoll tcp client
 * @author Jian
 * @date 2025-01-21
 */
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
        EpollTcpClient epollTcpClient = new EpollTcpClient();
        epollTcpClient.bootstrap = new Bootstrap();
        epollTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        epollTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        epollTcpClient.bootstrap.channel(EpollSocketChannel.class);
        //禁用组包
        //epollTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return epollTcpClient;
    }


    @Override
    public AbstractTcpClient groupThreadNum(int threadNum) {
        IoHandlerFactory ioHandlerFactory = EpollIoHandler.newFactory();
        MultiThreadIoEventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(threadNum, ioHandlerFactory);
        this.bootstrap.group(eventLoopGroup);
        return this;
    }
}
