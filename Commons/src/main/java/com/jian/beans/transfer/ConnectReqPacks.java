package com.jian.beans.transfer;

import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class ConnectReqPacks extends BaseTransferPacks {

    /***
     * 当前通道hash
     */
    private Long thisChannelHash;

    /***
     * 目标通道has
     */
    private Long tarChannelHash;

    /***
     * 连接的端口
     */
    private Integer port;

    /***
     * 连接的地址长度
     */
    private Integer hostLen;

    /***
     * 连接的地址
     */
    private String host;

    public ConnectReqPacks() {
        setType(TYPE.CONNECT_REQ);
    }
}
