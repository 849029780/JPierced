package com.jian.beans.transfer;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * tcp数据传输包
 * @author Jian
 * @date 2022/4/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TcpTransferDataPacks extends BaseTransferPacks {

    /***
     * 目标通道hash
     */
    private long tarChannelHash;

    /***
     * 数据包
     */
    private ByteBuf datas;

    public TcpTransferDataPacks() {
        setType(TYPE.TCP_TRANSFER_DATA);
    }
}
