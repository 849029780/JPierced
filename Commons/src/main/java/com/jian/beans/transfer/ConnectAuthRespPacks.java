package com.jian.beans.transfer;

import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class ConnectAuthRespPacks extends BaseTransferPacks {

    /***
     * 客户标识
     */
    private Long key;

    /***
     * 状态
     * 1--成功
     * 2--失败
     */
    private Byte state;

    /***
     * 消息长度
     */
    private Integer msgLen;

    /***
     * 消息
     */
    private String msg;

    public interface STATE{
        /***
         * 成功
         */
        byte SUCCESS = 1;

        /***
         * 失败
         */
        byte FAIL = 2;
    }


    public ConnectAuthRespPacks() {
        setType(TYPE.CONNECT_AUTH_RESP);
    }
}
