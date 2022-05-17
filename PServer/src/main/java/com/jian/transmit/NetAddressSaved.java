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
public class NetAddressSaved extends NetAddressBase {

    public NetAddressSaved(NetAddress netAddress) {
        setHost(netAddress.getHost());
        setPort(netAddress.getPort());
        setProtocol(netAddress.getProtocol());
    }
}
