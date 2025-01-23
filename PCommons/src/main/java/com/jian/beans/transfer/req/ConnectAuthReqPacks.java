package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConnectAuthReqPacks extends BaseTransferPacks {

    /***
     * 客户标识
     */
    private long key;

    /***
     * 密码字符长度
     */
    private int pwdLen;

    /***
     * 密码内容
     */
    private String pwd;

    public ConnectAuthReqPacks() {
        setType(TYPE.CONNECT_AUTH_REQ);
    }
}
