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
        setType(TYPE.ACK_CHANNEL_CONNECT_REQ);
    }
}
