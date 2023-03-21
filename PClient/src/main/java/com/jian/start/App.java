package com.jian.start;

import com.jian.beans.transfer.ConnectAuthReqPacks;
import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class App {

    public static void main(String[] args) {
        if (Objects.nonNull(args) && args.length > 0) {
            String arg = args[0];
            if (!StringUtil.isNullOrEmpty(arg)) {
                Config.dirPath = arg;
            }
        }
        if (Config.initConfig()) {
            String hostProperty = Constants.CONFIG.getProperty(Constants.HOST_PROPERTY_NAME);
            String portProperty = Constants.CONFIG.getProperty(Constants.PORT_PROPERTY_NAME);
            String keyProperty = Constants.CONFIG.getProperty(Constants.KEY_PROPERTY_NAME);
            String pwdProperty = Constants.CONFIG.getProperty(Constants.PWD_PROPERTY_NAME);

            if (StringUtil.isNullOrEmpty(hostProperty)) {
                log.error("server.host配置为空！启动失败！");
                return;
            }
            if (StringUtil.isNullOrEmpty(portProperty)) {
                log.error("server.port配置为空！启动失败！");
                return;
            }

            if (StringUtil.isNullOrEmpty(keyProperty)) {
                log.error("key配置为空！启动失败！");
                return;
            }
            if (StringUtil.isNullOrEmpty(pwdProperty)) {
                log.error("pwd配置为空！启动失败！");
                return;
            }
            Client.getRemoteInstance().connect(new InetSocketAddress(hostProperty, Integer.parseInt(portProperty)), (Consumer<ChannelFuture>) future -> {
                Channel channel = future.channel();
                ConnectAuthReqPacks connectAuthReqPacks = new ConnectAuthReqPacks();
                connectAuthReqPacks.setKey(Long.parseLong(keyProperty));
                connectAuthReqPacks.setPwd(pwdProperty);
                channel.writeAndFlush(connectAuthReqPacks);
            });
        }
    }

}
