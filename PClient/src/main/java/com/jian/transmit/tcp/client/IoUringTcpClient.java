package com.jian.transmit.tcp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;


/***
 * io_uring tcp client
 * @author Jian
 * @date 2025-01-21
 */
@Slf4j
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
        log.info("使用IoUring Channel.");
        if (Objects.isNull(AbstractTcpClient.EVENT_LOOP_GROUP)) {
            IoHandlerFactory ioHandlerFactory = IoUringIoHandler.newFactory();
            AbstractTcpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.THREAD_NUM, ioHandlerFactory);
        }
        IoUringTcpClient ioUringTcpClient = new IoUringTcpClient();
        ioUringTcpClient.bootstrap = new Bootstrap();
        ioUringTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        ioUringTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        ioUringTcpClient.bootstrap.channel(IoUringSocketChannel.class);
        ioUringTcpClient.bootstrap.group(AbstractTcpClient.EVENT_LOOP_GROUP);
        //禁用组包
        //ioUringTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        return ioUringTcpClient;
    }

}
