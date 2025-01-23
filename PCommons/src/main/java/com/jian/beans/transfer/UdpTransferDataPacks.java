package com.jian.beans.transfer;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * tcp数据传输包
 * @author Jian
 * @date 2025/01/22
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UdpTransferDataPacks extends BaseTransferPacks {

    /***
     * 数据来源端口
     */
    private Integer sourcePort;

    /***
     * 发送者ip长度
     */
    private Byte ipLen;

    /***
     * 发送者ip
     */
    private String senderHost;

    /***
     * 发送者端口
     */
    private Integer senderPort;

    /***
     * 数据包
     */
    private ByteBuf datas;

    public UdpTransferDataPacks() {
        setType(TYPE.UDP_TRANSFER_DATA);
    }
}
