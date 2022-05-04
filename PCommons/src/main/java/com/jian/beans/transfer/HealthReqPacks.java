package com.jian.beans.transfer;

import lombok.Data;

/**
 * 心跳请求包
 *
 * @author Jian
 * @date 2022/04/04
 */
@Data
public class HealthReqPacks extends BaseTransferPacks {

    /***
     * 消息Id
     */
    private Long msgId;

    public HealthReqPacks() {
        setType(TYPE.HEALTH_REQ);
    }
}
