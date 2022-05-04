package com.jian.beans.transfer;

import lombok.Data;

/***
 * 断开客户端报文
 * @author Jian
 * @date 2022/4/20
 */
@Data
public class DisConnectClientReqPacks extends BaseTransferPacks {

    /***
     * 1--是
     */
    private Byte code;


    public DisConnectClientReqPacks() {
        setType(TYPE.DIS_CONNECT_CLIENT);
    }
}
