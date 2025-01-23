package com.jian.beans.transfer.resp;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 心跳响应包
 *
 * @author Jian
 * @date 2022/04/04
 */
@EqualsAndHashCode(callSuper = true)
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
