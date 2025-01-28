package com.jian.transmit.udp.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;


/***
 * udp服务
 * @author Jian
 * @date 2024-12-30
 */
@Slf4j
public class UdpClient {

    private static final List<AbstractUdpClient> UDP_CLIENTS = List.of(new IoUringUdpClient(), new EpollUdpClient(), new KqueueUdpClient(), new NioUdpClient());

    private static AbstractUdpClient abstractUdpClient;

    public static AbstractUdpClient getUdpClient() {
        if (Objects.isNull(abstractUdpClient)) {
            for (AbstractUdpClient abstractUdpClient : UDP_CLIENTS) {
                if (abstractUdpClient.support()) {
                    UdpClient.abstractUdpClient = abstractUdpClient;
                    break;
                }
            }
        }
        return UdpClient.abstractUdpClient.getInstance();
    }


}
