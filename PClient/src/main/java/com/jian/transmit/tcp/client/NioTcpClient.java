package com.jian.transmit.tcp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;


/***
 * nio tcp client
 * @author Jian
 * @date 2025-01-21
 */
@Slf4j
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
        log.info("使用Nio Channel.");
        if (Objects.isNull(AbstractTcpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
            AbstractTcpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.THREAD_NUM, ioHandlerFactory);
        }
        NioTcpClient nioTcpClient = new NioTcpClient();
        nioTcpClient.bootstrap = new Bootstrap();
        nioTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        nioTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        nioTcpClient.bootstrap.channel(NioSocketChannel.class);
        nioTcpClient.bootstrap.group(AbstractTcpClient.EVENT_LOOP_GROUP);
        //禁用组包
        //nioTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return nioTcpClient;
    }

}
