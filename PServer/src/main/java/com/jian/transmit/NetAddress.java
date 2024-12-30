package com.jian.transmit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/04
 */
@EqualsAndHashCode(callSuper = true)
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
