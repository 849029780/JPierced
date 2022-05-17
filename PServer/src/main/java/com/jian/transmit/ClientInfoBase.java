package com.jian.transmit;

import lombok.Data;

@Data
public class ClientInfoBase {

    /***
     * 客户标识
     */
    private Long key;

    /***
     * 密码
     */
    private String pwd;

    /***
     * 客户名
     */
    private String name;

}
