package com.jian.commons;

import lombok.Data;

@Data
public class ClientConfig {

    private String serverHost;
    private Integer serverPort;
    private String key;
    private String pwd;

    private Boolean useSsl;

}
