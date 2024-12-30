package com.jian.commons;

import lombok.Data;

@Data
public class ServerConfig {

    private Login login;
    private Transmit transmit;
    private Web web;

    @Data
    public static class Login {
        private String username;
        private String password;
    }

    @Data
    public static class Transmit {
        private Integer port;
        private Boolean useSsl;
        private Integer ackPort;
    }

    @Data
    public static class Web {
        private Integer port;
        private Boolean useHttps;
    }


}
