package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * 连接请求
 * @author Jian
 * @date 2022/4/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConnectReqPacks extends BaseTransferPacks {

    /***
     * 当前通道hash
     */
    private long sourceChannelHash;

    /***
     * 目标通道has
     */
    private long tarChannelHash;

    /***
     * 连接的端口
     */
    private int port;

    /***
     * 协议类型
     * 1--tcp
     * 2--http
     * 3--https
     */
    private byte protocol;

    /***
     * 连接的地址长度
     */
    private int hostLen;

    /***
     * 连接的地址
     */
    private String host;

    public ConnectReqPacks() {
        setType(TYPE.CONNECT_REQ);
    }


    public interface Protocol{
        byte TCP = 1;
        byte HTTP = 2;
        byte HTTPS = 3;
    }

}
