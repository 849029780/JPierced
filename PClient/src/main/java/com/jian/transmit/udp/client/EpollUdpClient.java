package com.jian.transmit.udp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;

import java.util.Objects;

public class EpollUdpClient extends AbstractUdpClient {

    @Override
    public boolean support() {
        return Epoll.isAvailable();
    }

    @Override
    public AbstractUdpClient getInstance() {
        if (Objects.isNull(AbstractUdpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = EpollIoHandler.newFactory();
            AbstractUdpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.UDP_THREAD_NUM, ioHandlerFactory);
        }
        EpollUdpClient epollUdpClient = new EpollUdpClient();
        epollUdpClient.bootstrap = new Bootstrap();
        epollUdpClient.bootstrap.channel(EpollDatagramChannel.class);
        epollUdpClient.bootstrap.group(AbstractUdpClient.EVENT_LOOP_GROUP);
        return epollUdpClient;
    }


}
