package com.jian.transmit.udp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;

import java.util.Objects;

public class KqueueUdpClient extends AbstractUdpClient {

    @Override
    public boolean support() {
        return KQueue.isAvailable();
    }

    @Override
    public AbstractUdpClient getInstance() {
        if (Objects.isNull(AbstractUdpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = KQueueIoHandler.newFactory();
            AbstractUdpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.UDP_THREAD_NUM, ioHandlerFactory);
        }
        EpollUdpClient epollUdpClient = new EpollUdpClient();
        epollUdpClient.bootstrap = new Bootstrap();
        epollUdpClient.bootstrap.channel(KQueueDatagramChannel.class);
        epollUdpClient.bootstrap.group(AbstractUdpClient.EVENT_LOOP_GROUP);
        return epollUdpClient;
    }
}
