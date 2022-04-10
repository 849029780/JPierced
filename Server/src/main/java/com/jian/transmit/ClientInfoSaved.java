package com.jian.transmit;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class ClientInfoSaved {

    /***
     * 客户标识
     */
    private Long key;

    /***
     * 密码
     */
    private String pwd;

    /***
     * 客户名
     */
    private String name;

    /***
     * 需要在服务端映射的端口及被穿透机器上的地址和端口
     */
    private ConcurrentHashMap<Integer, NetAddress> portMappingAddress = new ConcurrentHashMap<>();


    public ClientInfoSaved() {

    }

    public ClientInfoSaved(ClientInfo clientInfo) {
        setKey(clientInfo.getKey());
        setPwd(clientInfo.getPwd());
        setName(clientInfo.getName());
        setPortMappingAddress(clientInfo.getPortMappingAddress());
    }

}
