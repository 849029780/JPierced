package com.jian.start;

import com.jian.commons.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class Server {

    private ServerBootstrap serverBootstrap;

    private Server() {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(Constants.BOSS_EVENT_LOOP_GROUP, Constants.WORK_EVENT_LOOP_GROUP);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        serverBootstrap.channel(NioServerSocketChannel.class);
    }

    public static Server getInstance() {
        Server server = new Server();
        return server;
    }

    public static Server getInstance(ChannelInitializer channelInitializer) {
        Server server = getInstance();
        server.serverBootstrap.childHandler(channelInitializer);
        return server;
    }

    public ChannelFuture listen(Integer port) {
        return serverBootstrap.bind(port);
    }

}
