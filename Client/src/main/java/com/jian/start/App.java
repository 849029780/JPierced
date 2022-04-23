package com.jian.start;

import com.jian.beans.transfer.ConnectAuthReqPacks;
import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
@Slf4j
public class App {

    public static void main(String[] args) {
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

            try {
                Constants.LOCAL_SSL_CONTEXT = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                Constants.REMOTE_SSL_CONTEXT = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                log.error("初始化SSL Context错误！", e);
                return;
            }

            Client.getRemoteInstance().connect(new InetSocketAddress(hostProperty, Integer.parseInt(portProperty)), (Consumer<ChannelFuture>) future -> {
                Channel channel = future.channel();
                ConnectAuthReqPacks connectAuthReqPacks = new ConnectAuthReqPacks();
                connectAuthReqPacks.setKey(Long.valueOf(keyProperty));
                connectAuthReqPacks.setPwd(pwdProperty);
                channel.writeAndFlush(connectAuthReqPacks);
            });
        }
    }

}
