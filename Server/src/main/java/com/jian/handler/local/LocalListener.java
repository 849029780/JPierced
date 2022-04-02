package com.jian.handler.local;

import com.jian.beans.Client;
import com.jian.beans.transfer.ConnectAuthRespPacks;
import com.jian.commons.Constants;
import com.jian.start.Server;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class LocalListener {

    public void listen(Set<Integer> ports, Client client){
        StringBuffer stringBuffer = new StringBuffer();
        CountDownLatch countDownLatch = new CountDownLatch(ports.size());
        for (Integer port : ports) {
            Server.getInstance(Constants.LOCAL_CHANNEL_INITIALIZER).listen(port).addListener(future -> {
                countDownLatch.countDown();
                ChannelFuture future1 = (ChannelFuture) future;
                Channel channel1 = future1.channel();

                InetSocketAddress socketAddress = (InetSocketAddress)channel1.localAddress();
                int port1 = socketAddress.getPort();
                stringBuffer.append("端口：");
                stringBuffer.append(port1);
                if (future1.isSuccess()) {
                    stringBuffer.append("监听成功；");
                    client.getListenPortMap().put(port1, channel1);
                } else {
                    if(future1.cause() instanceof BindException){
                        stringBuffer.append("监听失败，该端口已被使用；");
                    }else{
                        stringBuffer.append("监听失败；");
                    }
                }
            });
        }
        countDownLatch.getCount();
        ConnectAuthRespPacks connectAuthRespPacks = new ConnectAuthRespPacks();
        connectAuthRespPacks.setState(ConnectAuthRespPacks.STATE.SUCCESS);
        connectAuthRespPacks.setKey(client.getKey());
        connectAuthRespPacks.setMsg(stringBuffer.toString());
        client.getRemoteChannel().writeAndFlush(connectAuthRespPacks);
    }

}
