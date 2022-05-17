package com.jian.beans.transfer;

import lombok.Data;

/***
 * 断开连接请求
 * @author Jian
 * @date 2022/4/2
 */
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
