package com.jian.beans.transfer;

import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class ConnectRespPacks extends BaseTransferPacks {

    /***
     * 服务端当前的通道hash
     */
    private Long thisChannelHash;

    /***
     * 服务端目标的通道hash
     */
    private Long tarChannelHash;

    /***
     * 连接状态
     * 1--连接成功
     * 2--连接失败
     */
    private Byte state;

    /***
     * 消息长度
     */
    private Integer msgLen;

    /***
     * 消息内容
     */
    private String msg;


    public ConnectRespPacks() {
        setType(TYPE.CONNECT_RESP);
    }

    public interface STATE {
        /***
         * 成功
         */
        byte SUCCESS = 1;

        /***
         * 失败
         */
        byte FAIL = 2;
    }

}
