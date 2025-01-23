package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UdpPortMappingRemReqPacks extends BaseTransferPacks {

    public UdpPortMappingRemReqPacks() {
        super.setType(TYPE.UDP_PORT_MAPPING_REM_REQ);
    }

    /***
     * 映射端口
     */
    private Integer sourcePort;


}
