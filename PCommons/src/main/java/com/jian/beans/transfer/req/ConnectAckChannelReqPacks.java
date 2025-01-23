package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * 活动连接请求
 */
@EqualsAndHashCode(callSuper = true)
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
