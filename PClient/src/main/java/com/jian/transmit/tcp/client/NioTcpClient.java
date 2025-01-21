package com.jian.transmit.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;


/***
 * nio tcp client
 * @author Jian
 * @date 2025-01-21
 */
public class NioTcpClient extends AbstractTcpClient {

    /***
     * 所有系统都支持nio
     */
    @Override
    public boolean support() {
        return true;
    }


    @Override
    public AbstractTcpClient getInstance() {
        NioTcpClient nioTcpClient = new NioTcpClient();
        nioTcpClient.bootstrap = new Bootstrap();
        nioTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        nioTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        nioTcpClient.bootstrap.channel(NioSocketChannel.class);
        //禁用组包
        //nioTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return nioTcpClient;
    }


    @Override
    public AbstractTcpClient groupThreadNum(int threadNum) {
        IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
        MultiThreadIoEventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(threadNum, ioHandlerFactory);
        this.bootstrap.group(eventLoopGroup);
        return this;
    }
}
