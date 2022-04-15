package com.jian.start;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class Client {

    private Bootstrap bootstrap;

    /***
     * 保存
     */
    private InetSocketAddress connectInetSocketAddress;

    /***
     * 保存连接成功后发送的消息
     */
    private GenericFutureListener futureListener;

    /***
     * 重连过程
     */
    private Consumer<Future> reconnectConsumer;

    /***
     * 是否可进行重连，只有登录成功后断开连接才允许重连，别的操作不允许重连
     */
    private boolean canReconnect = false;

    /***
     * 重连次数
     */
    private AtomicReference<Integer> recount = new AtomicReference<>(0);

    /***
     * 重连次数
     * @return
     */
    public AtomicReference<Integer> getRecount() {
        return recount;
    }

    private Client() {
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(Constants.WORK_EVENT_LOOP_GROUP);
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        this.bootstrap.channel(Epoll.isAvailable() ? EpollSocketChannel.class : KQueue.isAvailable() ? KQueueSocketChannel.class : NioSocketChannel.class);
    }

    public boolean isCanReconnect() {
        return canReconnect;
    }

    public void setCanReconnect(boolean canReconnect) {
        this.canReconnect = canReconnect;
    }

    public static Client getInstance() {
        return new Client();
    }

    public static Client getInstance(ChannelInitializer initializer) {
        Client client = getInstance();
        client.bootstrap.handler(initializer);
        return client;
    }

    public static Client getLocalInstance() {
        return getInstance(Constants.LOCAL_CHANNEL_INITIALIZER);
    }


    public static Client getRemoteInstance() {
        return getInstance(Constants.REMOTE_CHANNEL_INITIALIZER);
    }

    public ChannelFuture connect(InetSocketAddress socketAddress) {
        connectInetSocketAddress = socketAddress;
        ChannelFuture channelFuture = this.bootstrap.connect(socketAddress);
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                ChannelFuture channelFuture1 = (ChannelFuture) future;
                Channel channel = channelFuture1.channel();
                channel.attr(Constants.CLIENT_KEY).set(this);
            }
        });
        return channelFuture;
    }


    public ChannelFuture connect(InetSocketAddress socketAddress, Consumer<Future> successFuture) {
        ChannelFuture channelFuture = connect(socketAddress);
        reconnectConsumer = future -> {
            if (canReconnect) {
                Integer count = recount.get();
                count++;
                recount.set(count);
                if (count > Constants.RECONNECT_MAX_COUNT) {
                    log.info("已超过最大重连次数:{}次，已退出重连..", Constants.RECONNECT_MAX_COUNT);
                    System.exit(0);
                    return;
                }
                try {
                    Thread.sleep(Duration.ofSeconds(Constants.RECONNECT_DELAY).toMillis());
                } catch (InterruptedException e) {
                    log.error("重连延迟错误..", e);
                }
                log.info("连接服务失败！正在尝试第:{}次,重连:{}，", count, this.connectInetSocketAddress);
                connect(socketAddress, futureListener);
            } else {
                log.info("连接服务失败！");
                System.exit(0);
            }
        };

        futureListener = future -> {
            if (future.isSuccess()) {
                successFuture.accept(future);
            } else {
                reconnectConsumer.accept(future);
            }
        };
        channelFuture.addListener(futureListener);
        return channelFuture;
    }


    public ChannelFuture connect(InetSocketAddress socketAddress, GenericFutureListener futureOnSuccess) {
        ChannelFuture channelFuture = connect(socketAddress);
        Optional.ofNullable(futureOnSuccess).ifPresent(fs -> channelFuture.addListener(futureOnSuccess));
        return channelFuture;
    }

    /***
     * 重连
     * @return
     */
    public ChannelFuture reConnct() {
        return connect(this.connectInetSocketAddress, (GenericFutureListener) future -> reconnectConsumer.accept(future));
    }

}
