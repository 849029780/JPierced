package com.jian.beans;

import io.netty.channel.Channel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UdpSenderChannelInfo {

    /***
     * udp本地通道
     */
    private Channel channel;

    /***
     * 通道数据收发时间
     */
    private LocalDateTime time;

}
