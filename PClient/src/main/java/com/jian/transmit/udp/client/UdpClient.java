package com.jian.transmit.udp.client;

import lombok.extern.slf4j.Slf4j;

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
                }
            }
        }
        return UdpClient.abstractUdpClient.getInstance();
    }


}
