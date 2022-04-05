package com.jian.transmit;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/04
 */
@Data
@AllArgsConstructor
public class NetAddress {

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
