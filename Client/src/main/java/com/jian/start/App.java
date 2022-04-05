package com.jian.start;

import com.jian.beans.transfer.ConnectAuthReqPacks;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class App {

    public static void main(String[] args) {
        Client.getRemoteInstance().connect(new InetSocketAddress("localhost", 9999),(Consumer<Future>) future -> {
            ChannelFuture future1 = (ChannelFuture) future;
            Channel channel = future1.channel();
            ConnectAuthReqPacks connectAuthReqPacks = new ConnectAuthReqPacks();
            connectAuthReqPacks.setKey(123456l);
            connectAuthReqPacks.setPwd("xxx");
            channel.writeAndFlush(connectAuthReqPacks);
        });
    }

}
