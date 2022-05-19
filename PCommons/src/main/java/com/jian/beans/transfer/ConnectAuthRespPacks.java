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
    private long key;

    /***
     * 状态
     * 1--成功
     * 2--失败
     */
    private byte state;

    /***
     * ack端口号
     */
    private int ackPort;

    /***
     * 消息长度
     */
    private int msgLen;

    /***
     * 消息
     */
    private String msg;


    public ConnectAuthRespPacks() {
        setType(TYPE.CONNECT_AUTH_RESP);
    }
}
