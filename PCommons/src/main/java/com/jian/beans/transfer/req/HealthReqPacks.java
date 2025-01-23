package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 心跳请求包
 *
 * @author Jian
 * @date 2022/04/04
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HealthReqPacks extends BaseTransferPacks {

    /***
     * 消息Id
     */
    private long msgId;

    public HealthReqPacks() {
        setType(TYPE.HEALTH_REQ);
    }
}
