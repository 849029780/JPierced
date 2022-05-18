package com.jian.beans.transfer;

import lombok.Data;

/***
 * 活动连接请求
 */
@Data
public class ConnectAckChannelReqPacks extends BaseTransferPacks {

    /***
     * 客户端key
     */
    private long key;


    public ConnectAckChannelReqPacks() {
        setType(TYPE.ACTIVE_CHANNEL_REQ);
    }
}
