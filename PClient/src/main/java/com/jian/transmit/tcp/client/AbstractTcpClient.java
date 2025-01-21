package com.jian.transmit.tcp.client;

import com.jian.commons.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;


/***
 * abstract tcpClient
 * @author Jian
 * @date 2025-01-21
 */
@Slf4j
public abstract class AbstractTcpClient {

    Bootstrap bootstrap;

    public abstract boolean support();

    public abstract AbstractTcpClient getInstance();

    public abstract AbstractTcpClient groupThreadNum(int threadNum);

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


    public AbstractTcpClient localChannelInitializer() {
        this.handler(Constants.LOCAL_CHANNEL_INITIALIZER);
        this.groupThreadNum(Constants.THREAD_NUM);
        return this;
    }

    public AbstractTcpClient localHttpsChannelInitializer() {
        this.handler(Constants.LOCAL_HTTPS_CHANNEL_INITIALIZER);
        this.groupThreadNum(Constants.THREAD_NUM);
        return this;
    }

    public AbstractTcpClient remoteChannelInitializer() {
        this.handler(Constants.REMOTE_CHANNEL_INITIALIZER);
        this.groupThreadNum(Constants.THREAD_NUM);
        return this;
    }

    public AbstractTcpClient remoteAckInitializer() {
        this.handler(Constants.REMOTE_ACK_CHANNEL_INITIALIZER);
        this.groupThreadNum(Constants.ACK_WORK_THREAD_NUM);
        return this;
    }


    public AbstractTcpClient handler(ChannelInitializer<Channel> initializer) {
        this.bootstrap.handler(initializer);
        return this;
    }

    public <T> AbstractTcpClient option(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
        return this;
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
                log.warn("连接服务:{}失败！", this.connectInetSocketAddress, future.cause());
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
