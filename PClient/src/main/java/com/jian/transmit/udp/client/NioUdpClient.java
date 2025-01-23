package com.jian.transmit.udp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.Objects;

public class NioUdpClient extends AbstractUdpClient {

    @Override
    public boolean support() {
        return true;
    }

    @Override
    public AbstractUdpClient getInstance() {
        if (Objects.isNull(AbstractUdpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
            AbstractUdpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.UDP_THREAD_NUM, ioHandlerFactory);
        }
        EpollUdpClient epollUdpClient = new EpollUdpClient();
        epollUdpClient.bootstrap = new Bootstrap();
        epollUdpClient.bootstrap.channel(NioDatagramChannel.class);
        epollUdpClient.bootstrap.group(AbstractUdpClient.EVENT_LOOP_GROUP);
        return epollUdpClient;
    }

}
