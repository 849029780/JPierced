package com.jian.beans.transfer;

import lombok.Data;

@Data
public class ConnectAckChannelRespPacks extends BaseTransferPacks {

    /***
     * 状态
     * 1--成功
     * 2--失败
     */
    private byte state;

    /***
     * 消息长度
     */
    private int msgLen;

    /***
     * 消息内容
     */
    private String msg;

    public ConnectAckChannelRespPacks() {
        setType(TYPE.ACK_CHANNEL_CONNECT_RESP);
    }
}
