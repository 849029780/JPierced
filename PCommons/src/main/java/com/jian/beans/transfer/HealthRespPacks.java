package com.jian.beans.transfer;

import lombok.Data;

/**
 * 心跳响应包
 *
 * @author Jian
 * @date 2022/04/04
 */
@Data
public class HealthRespPacks extends BaseTransferPacks {

    /***
     * 心跳包id
     */
    private long msgId;

    public HealthRespPacks() {
        setType(TYPE.HEALTH_RESP);
    }
}
