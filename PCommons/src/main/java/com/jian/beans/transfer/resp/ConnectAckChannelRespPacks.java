package com.jian.beans.transfer.resp;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
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
