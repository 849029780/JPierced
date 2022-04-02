package com.jian.start;

import com.jian.commons.Constants;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class App {


    public static void main(String[] args) {
        startServer();
        startProtocolServer();
    }

    public static void startServer() {
        Server.getInstance(Constants.LOCAL_CHANNEL_INITIALIZER).listen(5555).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("启动对外服务成功..");
            } else {
                System.err.println("启动对外服务失败..");
            }
        });
    }


    public static void startProtocolServer() {
        Server.getInstance(Constants.REMOTE_CHANNEL_INITIALIZER).listen(6666).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("启动传输服务成功..");
            } else {
                System.err.println("启动传输服务失败..");
            }
        });
    }


}
