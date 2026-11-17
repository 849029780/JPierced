package com.jian.transmit.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;

/***
 * tcpClients
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class TcpClient {

    private static final List<AbstractTcpClient> TCP_CLIENTS = List.of(new IoUringTcpClient(), new EpollTcpClient(), new KqueueTcpClient(), new NioTcpClient());

    private static AbstractTcpClient abstractTcpClient;

    private static volatile Bootstrap localTcpBootstrap;
    private static volatile Bootstrap localHttpsBootstrap;

    public static AbstractTcpClient getTcpClient() {
        if (Objects.isNull(abstractTcpClient)) {
            for (AbstractTcpClient abstractTcpClient : TCP_CLIENTS) {
                if (abstractTcpClient.support()) {
                    TcpClient.abstractTcpClient = abstractTcpClient;
                    break;
                }
            }
        }
        return TcpClient.abstractTcpClient.getInstance();
    }

    public static Bootstrap getLocalTcpBootstrap() {
        if (localTcpBootstrap == null) {
            synchronized (TcpClient.class) {
                if (localTcpBootstrap == null) {
                    AbstractTcpClient client = getTcpClient();
                    client.localChannelInitializer()
                            .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                            .option(ChannelOption.AUTO_READ, Boolean.FALSE);
                    localTcpBootstrap = client.getBootstrap();
                }
            }
        }
        return localTcpBootstrap;
    }

    public static Bootstrap getLocalHttpsBootstrap() {
        if (localHttpsBootstrap == null) {
            synchronized (TcpClient.class) {
                if (localHttpsBootstrap == null) {
                    AbstractTcpClient client = getTcpClient();
                    client.localHttpsChannelInitializer()
                            .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                            .option(ChannelOption.AUTO_READ, Boolean.FALSE);
                    localHttpsBootstrap = client.getBootstrap();
                }
            }
        }
        return localHttpsBootstrap;
    }

    /***
     * 建立本地服务连接（复用单例 Bootstrap，避免高频创建 Bootstrap 及其配置对象）
     */
    public static ChannelFuture connectLocal(SocketAddress address, boolean isHttps) {
        return (isHttps ? getLocalHttpsBootstrap() : getLocalTcpBootstrap()).connect(address);
    }

}
