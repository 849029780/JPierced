package com.jian.handler.local;

import com.jian.start.ClientConnectInfo;
import com.jian.beans.transfer.ConnectAuthRespPacks;
import com.jian.commons.Constants;
import com.jian.start.Server;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class LocalListener {

    public void listen(Set<Integer> ports, ClientConnectInfo clientConnectInfo) {
        if (!clientConnectInfo.isOnline()) {
            log.info("客户端key:{}，当前不在线，暂不可监听端口！", clientConnectInfo.getKey());
            return;
        }
        log.error("客户端key:{}，需要监听端口:{}", clientConnectInfo.getKey(), ports);
        StringBuffer stringBuffer = new StringBuffer();
        CountDownLatch countDownLatch = new CountDownLatch(ports.size());
        for (Integer port : ports) {
            Server.getInstance(Constants.LOCAL_CHANNEL_INITIALIZER).listen(port).addListener(future -> {
                countDownLatch.countDown();
                ChannelFuture future1 = (ChannelFuture) future;
                Channel channel1 = future1.channel();

                InetSocketAddress socketAddress = (InetSocketAddress) channel1.localAddress();
                int port1 = socketAddress.getPort();
                stringBuffer.append("端口：");
                stringBuffer.append(port1);
                if (future1.isSuccess()) {
                    stringBuffer.append("监听成功；");
                    log.info("客户端key：{}，监听端口:{}成功！", clientConnectInfo.getKey(), port1);
                    clientConnectInfo.getListenPortMap().put(port1, channel1);
                    Constants.PORT_MAPPING_CLIENT.put(port1, clientConnectInfo);
                } else {
                    if (future1.cause() instanceof BindException) {
                        stringBuffer.append("监听失败，该端口已被使用；");
                        log.info("客户端key：{}，监听端口:{}失败！该端口已被占用！", clientConnectInfo.getKey(), port1);
                    } else {
                        stringBuffer.append("监听失败；");
                        log.info("客户端key：{}，监听端口:{}失败！", clientConnectInfo.getKey(), port1);
                    }
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("客户端信息:{}，监听端口时CountDownLatch错误！", clientConnectInfo, e);
        }
        ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
        connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.SUCCESS);
        connectAuthRespPacks.setKey(clientConnectInfo.getKey());
        String listenInfo = stringBuffer.toString();
        log.info("客户端key:{},{}", clientConnectInfo.getKey(), listenInfo);
        connectAuthRespPacks.setMsg(listenInfo);
        clientConnectInfo.getRemoteChannel().writeAndFlush(connectAuthRespPacks);
    }

}
