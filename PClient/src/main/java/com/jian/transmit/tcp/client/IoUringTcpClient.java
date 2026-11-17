package com.jian.transmit.tcp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
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
        //return IoUring.isAvailable();
        return false;
    }


    @Override
    public AbstractTcpClient getInstance() {
        if (AbstractTcpClient.EVENT_LOOP_GROUP == null) {
            synchronized (AbstractTcpClient.class) {
                if (AbstractTcpClient.EVENT_LOOP_GROUP == null) {
                    log.info("使用IoUring Channel.");
                    IoHandlerFactory ioHandlerFactory = IoUringIoHandler.newFactory();
                    AbstractTcpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.THREAD_NUM, ioHandlerFactory);
                }
            }
        }
        IoUringTcpClient ioUringTcpClient = new IoUringTcpClient();
        ioUringTcpClient.bootstrap = new Bootstrap();
        ioUringTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        ioUringTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        ioUringTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        ioUringTcpClient.bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new io.netty.channel.WriteBufferWaterMark(128 * 1024, 512 * 1024));
        ioUringTcpClient.bootstrap.channel(IoUringSocketChannel.class);
        ioUringTcpClient.bootstrap.group(AbstractTcpClient.EVENT_LOOP_GROUP);
        return ioUringTcpClient;
    }

}
