package com.jian.commons;

import lombok.Data;

@Data
public class ServerConfig {

    private Transmit transmit;
    private Web web;

    @Data
    public static class Transmit {
        private Integer port;
        private Boolean useSsl;
        private Integer ackPort;
    }

    @Data
    public static class Web {
        private String defUsername;
        private String defPassword;

        private Integer port;
        private Boolean useHttps;
    }


}
