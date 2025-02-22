package com.jian.transmit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentHashMap;

/***
 * 客户连接信息
 * @author Jian
 * @date 2022/4/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClientInfo extends ClientInfoBase {
    /***
     * 客户端连接通道
     */
    @JsonIgnore
    private Channel remoteChannel;

    /***
     * ack通道
     */
    @JsonIgnore
    private Channel ackChannel;

    /***
     * 是否在线 默认false
     */
    private boolean isOnline = false;

    /***
     * 需要在服务端映射的端口及被穿透机器上的地址和端口
     */
    private ConcurrentHashMap<Integer, NetAddress> portMappingAddress = new ConcurrentHashMap<>();

    /***
     * 监听的端口 对应通道 客户端在线时才有连接数据，否则为空
     */
    @JsonIgnore
    private ConcurrentHashMap<Integer, Channel> listenPortMap = new ConcurrentHashMap<>();

    /***
     * 该客户端监听的端口上所有连接的通道
     */
    @JsonIgnore
    private ConcurrentHashMap<Long, Channel> connectedMap = new ConcurrentHashMap<>();

}
