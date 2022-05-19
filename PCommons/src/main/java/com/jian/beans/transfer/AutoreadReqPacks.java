package com.jian.beans.transfer;

import lombok.Data;

@Data
public class AutoreadReqPacks extends BaseTransferPacks {

    /***
     * 目标通道
     */
    private long tarChannelHash;

    /***
     * 是否自动读
     */
    private boolean isAutoRead;

    public AutoreadReqPacks() {
        setType(TYPE.ACK_AUTO_READ_REQ);
    }
}
