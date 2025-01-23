package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
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
