package com.jian.transmit.tcp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/***
 * kqueue tcp client
 * @author Jian
 * @date 2025-01-21
 */
@Slf4j
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
        if (AbstractTcpClient.EVENT_LOOP_GROUP == null) {
            synchronized (AbstractTcpClient.class) {
                if (AbstractTcpClient.EVENT_LOOP_GROUP == null) {
                    log.info("使用Kqueue Channel.");
                    IoHandlerFactory ioHandlerFactory = KQueueIoHandler.newFactory();
                    AbstractTcpClient.EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(Constants.THREAD_NUM, ioHandlerFactory);
                }
            }
        }
        KqueueTcpClient kqueueTcpClient = new KqueueTcpClient();
        kqueueTcpClient.bootstrap = new Bootstrap();
        kqueueTcpClient.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        kqueueTcpClient.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        kqueueTcpClient.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        kqueueTcpClient.bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new io.netty.channel.WriteBufferWaterMark(128 * 1024, 512 * 1024));
        kqueueTcpClient.bootstrap.channel(KQueueSocketChannel.class);
        kqueueTcpClient.bootstrap.group(AbstractTcpClient.EVENT_LOOP_GROUP);
        return kqueueTcpClient;
    }
}
