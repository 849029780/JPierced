package com.jian.start;

import com.jian.beans.transfer.req.ConnectAuthReqPacks;
import com.jian.commons.Constants;
import com.jian.transmit.tcp.client.TcpClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/***
 * 从这里开始
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
            String hostProperty = Constants.CONFIG.getServer().getHost();
            Integer portProperty = Constants.CONFIG.getServer().getPort();
            String keyProperty = Constants.CONFIG.getServer().getUsername();
            String pwdProperty = Constants.CONFIG.getServer().getPassword();


            if (StringUtil.isNullOrEmpty(hostProperty)) {
                log.error("server.host配置为空！启动失败！");
                return;
            }
            if (Objects.isNull(portProperty)) {
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
            startUdpCleaner();
            TcpClient.getTcpClient().remoteChannelInitializer().connect(new InetSocketAddress(hostProperty, portProperty), (Consumer<ChannelFuture>) future -> {
                Channel channel = future.channel();
                ConnectAuthReqPacks connectAuthReqPacks = new ConnectAuthReqPacks();
                connectAuthReqPacks.setKey(Long.parseLong(keyProperty));
                connectAuthReqPacks.setPwd(pwdProperty);
                channel.writeAndFlush(connectAuthReqPacks);
            });
        }
    }

    private static void startUdpCleaner() {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "udp-channel-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                Iterator<Map.Entry<String, com.jian.beans.UdpSenderChannelInfo>> iterator = Constants.SENDER_CHANNEL.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, com.jian.beans.UdpSenderChannelInfo> entry = iterator.next();
                    com.jian.beans.UdpSenderChannelInfo info = entry.getValue();
                    if (info.getTime() != null && Duration.between(info.getTime(), now).getSeconds() > 60) {
                        iterator.remove();
                        Channel ch = info.getChannel();
                        if (ch != null) {
                            Integer sourcePort = ch.attr(Constants.SOURCE_PORT).get();
                            if (sourcePort != null) {
                                List<Channel> list = Constants.UDP_SERVER_MAPPING_LOCAL_CHANNEL.get(sourcePort);
                                if (list != null) {
                                    list.remove(ch);
                                }
                            }
                            ch.close();
                            log.debug("UDP映射通道超时已释放: {}", entry.getKey());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("UDP通道清理任务异常", e);
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

}
