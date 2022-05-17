package com.jian.beans.transfer;

import lombok.Data;

@Data
public class MessageReqPacks extends BaseTransferPacks {

    /***
     * 消息长度
     */
    private int msgLen;

    /***
     * 消息
     */
    private String msg;


    public MessageReqPacks() {
        setType(TYPE.MESSAGE_REQ);
    }
}
