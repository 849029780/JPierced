package com.jian.transmit.handler.local.http;

import io.netty.util.AsciiString;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HttpHeader {

    /***
     * ascii header name
     */
    private AsciiString headerName;

    /***
     * header name
     */
    private String name;

    /***
     * header value
     */
    private String value;

    public HttpHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public HttpHeader(AsciiString headerName, String value) {
        this.headerName = headerName;
        this.value = value;
    }
}
