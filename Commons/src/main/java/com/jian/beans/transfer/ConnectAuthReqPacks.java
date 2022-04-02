package com.jian.beans.transfer;

import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class ConnectAuthReqPacks extends BaseTransferPacks {

    /***
     * 客户标识
     */
    private Long key;

    /***
     * 密码字符长度
     */
    private Integer pwdLen;

    /***
     * 密码内容
     */
    private String pwd;

    public ConnectAuthReqPacks() {
        setType(TYPE.CONNECT_AUTH_REQ);
    }
}
