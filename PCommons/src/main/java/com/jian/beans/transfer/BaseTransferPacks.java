package com.jian.beans.transfer;

import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class BaseTransferPacks {

    private int packSize;

    private byte type;

    public interface TYPE {
        /***
         * 连接请求
         */
        byte CONNECT_REQ = 1;

        /***
         * 连接响应
         */
        byte CONNECT_RESP = 2;

        /***
         * 断开本地连接请求
         */
        byte DIS_CONNECT_REQ = 3;

        /***
         * 客户端连接认证请求
         */
        byte CONNECT_AUTH_REQ = 4;

        /***
         * 客户端连接认证响应
         */
        byte CONNECT_AUTH_RESP = 5;

        /***
         * 传输数据
         */
        byte TRANSFER_DATA = 7;

        /***
         * 心跳请求
         */
        byte HEALTH_REQ = 8;

        /***
         * 心跳响应
         */
        byte HEALTH_RESP = 9;

        /***
         * 断开客户端连接
         */
        byte DIS_CONNECT_CLIENT = 10;

        /***
         * 发送消息请求
         */
        byte MESSAGE_REQ = 11;

        /***
         * 活动通道连接请求
         */
        byte ACTIVE_CHANNEL_REQ = 12;

        /***
         * 活动通道连接响应
         */
        byte ACTIVE_CHANNEL_RESP = 13;

        /***
         * 自动读消息请求
         */
        byte AUTO_READ_REQ = 14;

    }


    public interface STATE{
        /***
         * 成功
         */
        byte SUCCESS = 1;

        /***
         * 失败
         */
        byte FAIL = 2;
    }

}
