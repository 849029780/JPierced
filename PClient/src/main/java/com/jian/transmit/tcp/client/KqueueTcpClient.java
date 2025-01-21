package com.jian.transmit.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;

/***
 * kqueue tcp client
 * @author Jian
 * @date 2025-01-21
 */
public class KqueueTcpClient extends AbstractTcpClient {

    /***
     * 所有系统都支持nio
     */
    @Override
    public boolean support() {
        return KQueue.isAvailable();
    }


    @Override
    public AbstractTcpClient getInstance() {
        KqueueTcpClient kqueueTcpClient = new KqueueTcpClient();
        kqueueTcpClient.bootstrap = new Bootstrap();
        kqueueTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        kqueueTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        kqueueTcpClient.bootstrap.channel(KQueueSocketChannel.class);
        //禁用组包
        //kqueueTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return kqueueTcpClient;
    }


    @Override
    public AbstractTcpClient groupThreadNum(int threadNum) {
        IoHandlerFactory ioHandlerFactory = KQueueIoHandler.newFactory();
        MultiThreadIoEventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(threadNum, ioHandlerFactory);
        this.bootstrap.group(eventLoopGroup);
        return this;
    }
}
