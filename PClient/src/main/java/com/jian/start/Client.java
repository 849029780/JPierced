package com.jian.start;

import com.jian.commons.Constants;
import com.jian.utils.ChannelEventUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class Client {

    private final Bootstrap bootstrap;

    /***
     * 保存
     */
    private InetSocketAddress connectInetSocketAddress;

    /***
     * 连接成功后的操作
     */
    private Consumer<ChannelFuture> successFuture;

    /***
     * 是否可进行重连，只有登录成功后断开连接才允许重连，别的操作不允许重连
     */
    @Setter
    @Getter
    private boolean canReconnect = false;

    private Client() {
        this.bootstrap = new Bootstrap();
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000);
        this.bootstrap.channel(ChannelEventUtils.getSocketChannelClass());
        this.bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.TRUE);
    }

    public static Client getInstance() {
        return new Client();
    }

    public static Client getInstance(ChannelInitializer<Channel> initializer) {
        Client client = getInstance();
        client.bootstrap.handler(initializer);
        return client;
    }

    public <T> Client option(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
        return this;
    }

    public static Client getLocalInstance() {
        Client client = getInstance(Constants.LOCAL_CHANNEL_INITIALIZER);
        client.bootstrap.group(ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return client;
    }

    public static Client getLocalHttpsInstance() {
        Client client = getInstance(Constants.LOCAL_HTTPS_CHANNEL_INITIALIZER);
        client.bootstrap.group(ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return client;
    }


    public static Client getRemoteInstance() {
        Client client = getInstance(Constants.REMOTE_CHANNEL_INITIALIZER);
        client.bootstrap.group(ChannelEventUtils.getEventLoopGroup(Constants.THREAD_NUM));
        return client;
    }

    public static Client getRemoteAckInstance() {
        Client client = getInstance(Constants.REMOTE_ACK_CHANNEL_INITIALIZER);
        client.bootstrap.group(ChannelEventUtils.getEventLoopGroup(Constants.ACK_WORK_THREAD_NUM));
        return client;
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

    public void connect(InetSocketAddress socketAddress, Consumer<ChannelFuture> successFuture) {
        this.successFuture = successFuture;
        connect(socketAddress).addListener(future -> {
            if (future.isSuccess()) {
                successFuture.accept((ChannelFuture) future);
            } else if (canReconnect) {
                //重连
                Constants.CACHED_EXECUTOR_POOL.execute(this::reConnct);
            } else {
                log.warn("连接服务:{}失败！", this.connectInetSocketAddress);
                System.exit(0);
            }
        });
    }


    public ChannelFuture connect(InetSocketAddress socketAddress, GenericFutureListener<ChannelFuture> futureOnSuccess) {
        ChannelFuture channelFuture = connect(socketAddress);
        Optional.ofNullable(futureOnSuccess).ifPresent(fs -> channelFuture.addListener(futureOnSuccess));
        return channelFuture;
    }

    /***
     * 重连
     */
    public void reConnct() {
        log.warn("连接服务断开！{}秒后将进行重连:{}", Constants.RECONNECT_DELAY, this.connectInetSocketAddress);
        try {
            Thread.sleep(Duration.ofSeconds(Constants.RECONNECT_DELAY).toMillis());
        } catch (InterruptedException e) {
            log.error("重连延迟错误..", e);
        }
        connect(this.connectInetSocketAddress, this.successFuture);
    }

}
