package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
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
