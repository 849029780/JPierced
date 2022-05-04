package com.jian.beans.transfer;

import lombok.Data;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Data
public class BaseTransferPacks {

    private Integer packSize;

    private Byte type;

    public interface TYPE {
        /***
         * 连接请求
         */
        Byte CONNECT_REQ = 1;

        /***
         * 连接响应
         */
        Byte CONNECT_RESP = 2;

        /***
         * 断开本地连接请求
         */
        Byte DIS_CONNECT_REQ = 3;

        /***
         * 客户端连接认证请求
         */
        Byte CONNECT_AUTH_REQ = 4;

        /***
         * 客户端连接认证响应
         */
        Byte CONNECT_AUTH_RESP = 5;

        /***
         * 传输数据
         */
        Byte TRANSFER_DATA = 7;

        /***
         * 心跳请求
         */
        Byte HEALTH_REQ = 8;

        /***
         * 心跳响应
         */
        Byte HEALTH_RESP = 9;

        /***
         * 断开客户端连接
         */
        Byte DIS_CONNECT_CLIENT = 10;
    }

}
