package com.jian.start;

import com.jian.commons.Constants;
import com.jian.transmit.Server;
import com.jian.web.WebManagerVerticle;
import io.vertx.core.Vertx;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class App {


    public static void main(String[] args) {
        if (Config.initConfig()) {
            Config.initTransmitData();
            Server.listenRemote();
            Server.listenRemoteAck();
            Constants.VERTX = Vertx.vertx();
            Constants.VERTX.deployVerticle(new WebManagerVerticle()).onSuccess(deployId->Constants.VERTX_WEB_DEPLOY_ID = deployId);
        }
    }


}
