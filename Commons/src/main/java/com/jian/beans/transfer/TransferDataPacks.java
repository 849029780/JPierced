package com.jian.beans.transfer;

import io.netty.buffer.ByteBuf;
import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class TransferDataPacks extends BaseTransferPacks {

    /***
     * 目标通道hash
     */
    private Long targetChannelHash;

    /***
     * 数据包
     */
    private ByteBuf datas;

    public TransferDataPacks() {
        setType(TYPE.TRANSFER_DATA);
    }
}
