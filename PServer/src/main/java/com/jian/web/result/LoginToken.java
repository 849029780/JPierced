package com.jian.web.result;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginToken {

    private String token;

    private long expiresTimestamp;
}
