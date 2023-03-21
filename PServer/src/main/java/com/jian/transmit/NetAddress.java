package com.jian.transmit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.SocketAddress;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetAddress extends NetAddressBase {

    /***
     * 是否监听中
     */
    private boolean isListen;

    public NetAddress(String host, Integer port, Protocol protocol,Boolean cliUseHttps) {
        setHost(host);
        setPort(port);
        setProtocol(protocol);
        setCliUseHttps(cliUseHttps);
    }
}
