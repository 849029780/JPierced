package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * 断开客户端报文
 * @author Jian
 * @date 2022/4/20
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DisConnectClientReqPacks extends BaseTransferPacks {

    /***
     * 1--是
     */
    private byte code;


    public DisConnectClientReqPacks() {
        setType(TYPE.DIS_CONNECT_CLIENT);
    }
}
