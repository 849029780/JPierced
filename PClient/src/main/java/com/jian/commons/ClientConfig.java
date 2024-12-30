package com.jian.commons;

import lombok.Data;

@Data
public class ClientConfig {

    private Server server;

    @Data
    public static class Server{
        private String host;
        private Integer port;
        private String username;
        private String password;
        private Boolean useSsl;
    }

}
