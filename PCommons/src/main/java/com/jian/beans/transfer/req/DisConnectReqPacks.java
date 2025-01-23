package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * 断开连接请求
 * @author Jian
 * @date 2022/4/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DisConnectReqPacks extends BaseTransferPacks {

    /**
     * 目标通道hash
     */
    private long tarChannelHash;

    public DisConnectReqPacks() {
        setType(TYPE.DIS_CONNECT_REQ);
    }
}
