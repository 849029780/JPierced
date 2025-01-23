package com.jian.transmit.tcp.client;

import lombok.extern.slf4j.Slf4j;

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


}
