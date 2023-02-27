package com.jian.start;

import com.jian.commons.Constants;
import com.jian.transmit.Server;
import com.jian.web.WebManagerVerticle;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;

import java.util.Objects;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class App {


    public static void main(String[] args) {
        if (Objects.nonNull(args) && args.length > 0) {
            String arg = args[0];
            if (!StringUtil.isNullOrEmpty(arg)) {
                Config.dirPath = arg;
            }
        }
        if (Config.initConfig()) {
            Config.initTransmitData();
            Server.listenRemote();
            Server.listenRemoteAck();
            Constants.VERTX = Vertx.vertx();
            Constants.VERTX.deployVerticle(new WebManagerVerticle()).onSuccess(deployId -> Constants.VERTX_WEB_DEPLOY_ID = deployId);
        }
    }


}
