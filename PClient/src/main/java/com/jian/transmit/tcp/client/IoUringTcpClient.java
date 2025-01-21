package com.jian.transmit.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringSocketChannel;


/***
 * io_uring tcp client
 * @author Jian
 * @date 2025-01-21
 */
public class IoUringTcpClient extends AbstractTcpClient {

    /***
     * 所有系统都支持nio
     */
    @Override
    public boolean support() {
        return IoUring.isAvailable();
    }


    @Override
    public AbstractTcpClient getInstance() {
        IoUringTcpClient ioUringTcpClient = new IoUringTcpClient();
        ioUringTcpClient.bootstrap = new Bootstrap();
        ioUringTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        ioUringTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        ioUringTcpClient.bootstrap.channel(IoUringSocketChannel.class);
        //禁用组包
        //ioUringTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return ioUringTcpClient;
    }


    @Override
    public AbstractTcpClient groupThreadNum(int threadNum) {
        IoHandlerFactory ioHandlerFactory = IoUringIoHandler.newFactory();
        MultiThreadIoEventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(threadNum, ioHandlerFactory);
        this.bootstrap.group(eventLoopGroup);
        return this;
    }
}
