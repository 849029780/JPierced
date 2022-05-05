package com.jian.transmit;

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
public class NetAddress extends SocketAddress {

    private String host;

    private Integer port;

    //默认为tcp
    private Protocol protocol;

    public enum Protocol{
        TCP,
        HTTP,
        HTTPS
    }

}
