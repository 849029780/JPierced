package com.jian.transmit;

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
public class NetAddressSaved extends NetAddressBase {

    public NetAddressSaved(NetAddress netAddress) {
        setHost(netAddress.getHost());
        setPort(netAddress.getPort());
        setProtocol(netAddress.getProtocol());
        setCliUseHttps(netAddress.getCliUseHttps());
    }
}
