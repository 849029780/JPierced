package com.jian.transmit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ClientInfoSaved extends ClientInfoBase {

    /***
     * 需要在服务端映射的端口及被穿透机器上的地址和端口
     */
    private ConcurrentHashMap<Integer, NetAddressSaved> portMappingAddress = new ConcurrentHashMap<>();


    public ClientInfoSaved(ClientInfo clientInfo) {
        setKey(clientInfo.getKey());
        setPwd(clientInfo.getPwd());
        setName(clientInfo.getName());
        for (Map.Entry<Integer, NetAddress> integerNetAddressEntry : clientInfo.getPortMappingAddress().entrySet()) {
            Integer key = integerNetAddressEntry.getKey();
            NetAddress netAddress = integerNetAddressEntry.getValue();
            NetAddressSaved netAddressSaved = new NetAddressSaved(netAddress);
            portMappingAddress.put(key, netAddressSaved);
        }
    }

}
