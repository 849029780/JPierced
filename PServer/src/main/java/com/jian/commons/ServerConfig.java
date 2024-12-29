package com.jian.commons;

import lombok.Data;

@Data
public class ServerConfig {

    private String loginUsername;
    private String loginPwd;


    private Integer ackPort = 6210;
    private Integer transmitPort = 9999;
    private Integer webPort = 8000;

    private Boolean useHttps = false;

}
