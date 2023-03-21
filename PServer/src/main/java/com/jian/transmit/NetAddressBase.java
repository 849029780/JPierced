package com.jian.transmit;

import lombok.Data;

import java.net.SocketAddress;

@Data
public class NetAddressBase extends SocketAddress {


    private String host;

    private Integer port;

    /***
     * 默认为tcp
     */
    private Protocol protocol;

    /***
     * 客户端是否启用https
     */
    private Boolean cliUseHttps = Boolean.FALSE;

    public enum Protocol {
        TCP,
        HTTP,
        HTTPS
    }

}
