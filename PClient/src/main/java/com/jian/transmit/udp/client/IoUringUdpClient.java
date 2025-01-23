package com.jian.transmit.udp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;

import java.util.Objects;

public class IoUringUdpClient extends AbstractUdpClient {

    @Override
    public boolean support() {
        return IoUring.isAvailable();
    }

    @Override
    public AbstractUdpClient getInstance() {
        if (Objects.isNull(AbstractUdpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = IoUringIoHandler.newFactory();
            AbstractUdpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.UDP_THREAD_NUM, ioHandlerFactory);
        }
        EpollUdpClient epollUdpClient = new EpollUdpClient();
        epollUdpClient.bootstrap = new Bootstrap();
        epollUdpClient.bootstrap.channel(IoUringDatagramChannel.class);
        epollUdpClient.bootstrap.group(AbstractUdpClient.EVENT_LOOP_GROUP);
        return epollUdpClient;
    }
}
