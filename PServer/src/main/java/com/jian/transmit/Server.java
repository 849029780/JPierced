package com.jian.transmit;

import java.util.List;
import java.util.Objects;

public class Server {


    public static void listenLocal(List<NetAddress> addressList, ClientInfo clientInfo) {
        if(Objects.isNull(addressList) || addressList.isEmpty()) return;
        for (NetAddress netAddress : addressList) {
            NetAddressBase.Protocol protocol = netAddress.getProtocol();
            if(protocol == NetAddressBase.Protocol.UDP) {
                //UdpServer.
            }else{
                //TcpServer.listenLocal();
            }
        }


    }


}
