package com.jian.transmit;

import lombok.Data;

import java.net.SocketAddress;

@Data
public class NetAddressBase extends SocketAddress {


    private String host;

    private Integer port;

    //默认为tcp
    private Protocol protocol;

    public enum Protocol {
        TCP,
        HTTP,
        HTTPS
    }

}
