package com.jian.beans;

import io.netty.channel.Channel;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class Client {

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
     * 客户端连接通道
     */
    private Channel remoteChannel;

    /***
     * 是否在线 默认false
     */
    private boolean isOnline = false;

    /***
     * 需要在服务端映射的端口及被穿透机器上的地址和端口
     */
    private Map<Integer, InetSocketAddress> portMappingAddress;

    /***
     * 监听的端口 对应通道 客户端在线时才有连接数据，否则为空
     */
    private Map<Integer, Channel> listenPortMap = new ConcurrentHashMap<>();


}
