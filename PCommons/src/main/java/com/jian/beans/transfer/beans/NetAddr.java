package com.jian.beans.transfer.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetAddr {

    /***
     * 来源端口号
     */
    private Integer sourcePort;

    /***
     * 绑定的目标host
     */
    private String host;

    /***
     * 绑定的目标端口
     */
    private Integer port;

}
