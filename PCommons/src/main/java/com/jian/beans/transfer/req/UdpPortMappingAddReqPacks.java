package com.jian.beans.transfer.req;

import com.jian.beans.transfer.BaseTransferPacks;
import com.jian.beans.transfer.beans.NetAddr;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class UdpPortMappingAddReqPacks extends BaseTransferPacks {

    public UdpPortMappingAddReqPacks() {
        super.setType(TYPE.UDP_PORT_MAPPING_ADD_REQ);
    }

    /***
     * 数据包如下传输
     */
    private List<NetAddr> netAddrList;

    /***
     * 数量
     *//*
    private Byte count;

    *//***
     * 服务端udp端口
     *//*
    private Integer sourcePort;

    *//***
     * host长度
     *//*
    private Byte ipHostLen;

    *//***
     * host
     *//*
    private String ipHost;

    *//***
     * 端口号
     *//*
    private String port;*/


}
