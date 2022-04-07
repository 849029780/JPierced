package com.jian.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jian.beans.transfer.DisConnectReqPacks;
import com.jian.commons.Constants;
import com.jian.start.Config;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.transmit.Server;
import com.jian.utils.JsonUtils;
import com.jian.web.result.Page;
import com.jian.web.result.Result;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/04
 */
@Slf4j
public class WebManagerVerticle extends AbstractVerticle {

    HttpServer httpServer;

    /***
     * 登录页面
     */
    private final String LOGIN_PAGE_URI = "/page/login.html";

    /***
     * 登录请求
     */
    private final String LOGIN_URI = "/api/login";


    @Override
    public void start() {
        String webPort = Constants.CONFIG.getProperty(Constants.WEB_PORT_PROPERTY, Constants.DEF_WEB_PORT);
        Integer port = Integer.valueOf(webPort);
        Router router = routerManager();
        httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router).listen(port).onSuccess(han -> {
            log.info("web服务启动完成！端口:{}", port);
        }).onFailure(han -> {
            Throwable cause = han.getCause();
            log.error("web服务启动失败！端口:{}，错误原因:{}", port, cause);
        });
    }

    @Override
    public void stop() {
        httpServer.close();
    }

    public Router routerManager() {
        Router router = Router.router(this.vertx);
        ResponseContentTypeHandler responseContentTypeHandler = ResponseContentTypeHandler.create();
        //设置响应头Content-type为json
        router.route("/api/*").produces(HttpHeaderValues.APPLICATION_JSON.toString()).handler(responseContentTypeHandler);
        router.route()
                .handler(BodyHandler.create())
                .handler(han -> {
                    HttpServerRequest request = han.request();
                    String uri = request.uri();
                    //如果访问路径为登录页面或登录请求，则跳过权限认证(白名单)
                    if (uri.equals(LOGIN_PAGE_URI) || uri.equals(LOGIN_URI)) {
                        han.next();
                        return;
                    }
                    //获取请求token
                    String token = request.getHeader("token");
                    //token为空，则跳转到登录页
                    if (StringUtil.isNullOrEmpty(token)) {
                        //需要登录，跳转到登录页
                        han.reroute(HttpMethod.GET, LOGIN_PAGE_URI);
                    } else {
                        //token认证
                        if (checkToken(token)) {
                            //token正确
                            han.next();
                        } else {
                            //需要登录，跳转到登录页
                            han.reroute(HttpMethod.GET, LOGIN_PAGE_URI);
                        }
                    }
                });

        //静态页面
        router.route(HttpMethod.GET, "/page/*")
                .handler(responseContentTypeHandler)
                .produces(HttpHeaderValues.TEXT_HTML.toString())
                .handler(StaticHandler.create("static").setCachingEnabled(true));


        router.route(HttpMethod.POST, "/api/login").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = login(params);
            han.response().end(JsonUtils.toJson(result));
        });

        router.route(HttpMethod.GET, "/api/clientList").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = clienList(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/addClient").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = addClient(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/removeClient").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = removeClient(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/modifyClientPortMapping").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = modifyClientPortMapping(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/addClientPortMapping").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = addClientPortMapping(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/removeClientPortMapping").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = removeClientPortMapping(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/setTrasmitPort").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = setTrasmitPort(params);
            han.response().end(JsonUtils.toJson(result));
        });
        router.route(HttpMethod.POST, "/api/setWebPort").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = setWebPort(params);
            han.response().end(JsonUtils.toJson(result));
        });
        return router;
    }


    /***
     * 校验token
     * @param token
     * @return
     */
    public boolean checkToken(String token) {
        boolean isOk = false;
        try {
            DecodedJWT tokenDecode = JWT.decode(token);
            //验证token
            Constants.JWT_ALGORITHM.verify(tokenDecode);
            //失效时间戳
            Long expiresAt = tokenDecode.getClaim("expiresAt").asLong();
            //失效时间
            LocalDateTime expireTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAt), ZoneId.systemDefault());
            LocalDateTime now = LocalDateTime.now();
            //token是否过期
            boolean isBefore = expireTime.isAfter(now);
            //token不能过期
            if (isBefore) {
                isOk = true;
            }
        } catch (JWTDecodeException | SignatureVerificationException e) {
            //token错误
        }
        return isOk;
    }


    /***
     * 登录
     * @param params {"username":"", "pwd":""}
     * @return token
     */
    public Result login(JsonObject params) {
        String username = params.getString("username", "");
        String pwd = params.getString("pwd", "");
        String sysUserName = Constants.CONFIG.getProperty(Constants.LOGIN_USERNAME_PROPERTY, "");
        String sysPwd = Constants.CONFIG.getProperty(Constants.LOGIN_PWD_PROPERTY, "");
        Result result;
        //
        if (sysUserName.equals(username) && sysPwd.equals(pwd)) {
            LocalDateTime localDateTime = LocalDateTime.now().plusDays(7l);
            long expiresTimestamp = localDateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
            String token = JWT.create().withClaim("auther", "Jian").withKeyId("0508").withClaim("uname", username).withClaim("expiresAt", expiresTimestamp).sign(Constants.JWT_ALGORITHM);
            result = Result.SUCCESS(token, "登录成功！");
        } else {
            result = Result.FAIL("用户名或密码错误！");
        }
        return result;
    }


    /***
     * 客户端列表
     * @param params {"key":"数字去掉双引号", "name":"", "serverPort":"数字去掉双引号"}
     * @return
     */
    public Result clienList(JsonObject params) {
        Long key = params.getLong("key");
        String name = params.getString("name");
        Integer serverPort = params.getInteger("serverPort");

        Collection<ClientInfo> values = Constants.CLIENTS.values();

        List<ClientInfo> collectClient = values.stream().filter(cli -> {
            boolean isEq = true;
            if (Objects.nonNull(key)) {
                if (cli.getKey().equals(key)) {
                    isEq = true;
                } else {
                    isEq = false;
                }
            }

            if (isEq && !StringUtil.isNullOrEmpty(name)) {
                if (cli.getName().equals(name)) {
                    isEq = true;
                } else {
                    isEq = false;
                }
            }

            if (isEq && Objects.nonNull(serverPort)) {
                Map<Integer, NetAddress> portMappingAddress = cli.getPortMappingAddress();
                NetAddress netAddress = portMappingAddress.get(serverPort);
                if (Objects.nonNull(netAddress)) {
                    isEq = true;
                } else {
                    isEq = false;
                }
            }
            return isEq;
        }).collect(Collectors.toList());

        return Result.SUCCESS(Page.page(collectClient));
    }

    /***
     * 添加客户端
     * @param params {"key":"数字去掉双引号", "pwd":"xxx", "name":""}
     * @return
     */
    public Result addClient(JsonObject params) {
        Long key = params.getLong("key");
        String pwd = params.getString("pwd");
        String name = params.getString("name");

        if (Objects.isNull(key)) {
            return Result.FAIL("添加客户端失败！key为空！");
        }
        if (StringUtil.isNullOrEmpty(pwd)) {
            return Result.FAIL("添加客户端失败！pwd为空！");
        }
        if (StringUtil.isNullOrEmpty(name)) {
            return Result.FAIL("添加客户端失败！客户端名称为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);

        if (Objects.nonNull(clientInfo)) {
            return Result.FAIL("添加客户端失败！客户端key已存在！");
        }

        clientInfo = new ClientInfo();

        clientInfo.setKey(key);
        clientInfo.setName(name);
        clientInfo.setPwd(pwd);

        Constants.CLIENTS.put(key, clientInfo);
        //重新保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("添加客户端成功！");
    }

    /***
     * 移除客户端
     * @param params {"key":"数字去掉双引号"}
     * @return
     */
    public Result removeClient(JsonObject params) {
        Long key = params.getLong("key");
        if (Objects.isNull(key)) {
            return Result.FAIL("移除客户端失败！key为空！");
        }
        ClientInfo clientInfo = Constants.CLIENTS.remove(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("移除客户端失败！key不存在！");
        }
        //远程连接不为空，则该客户端在线，在线时需要将该客户端下线操作
        Channel remoteChannel = clientInfo.getRemoteChannel();
        Optional.ofNullable(remoteChannel).ifPresent(ChannelOutboundInvoker::close);
        //重新保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("移除客户端成功！");
    }


    /***
     * 添加映射
     * @param params {"key":"数字去掉双引号", "serverPort":"xxx", "host":"", "port":"数字去掉双引号", "protocol":"1或2 数字去掉双引号"}
     * @return
     */
    public Result addClientPortMapping(JsonObject params) {
        Long key = params.getLong("key");
        Integer serverPort = params.getInteger("serverPort");
        String host = params.getString("host");
        Integer port = params.getInteger("port");
        //协议类型，1--tcp，2--http，3--https(https暂未实现)
        Integer protocolType = params.getInteger("protocol");

        if (Objects.isNull(key)) {
            return Result.FAIL("映射端口失败！客户端key为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("当前客户端key不存在！映射端口失败！");
        }

        if (Objects.isNull(serverPort)) {
            return Result.FAIL("映射端口失败！服务端映射端口为空！");
        }

        if (StringUtil.isNullOrEmpty(host) || Objects.isNull(port)) {
            return Result.FAIL("映射端口失败！客户端映射的地址或端口为空！");
        }

        if (Objects.isNull(protocolType)) {
            return Result.FAIL("映射端口失败！映射端口协议类型为空！");
        }
        NetAddress.Protocol protocol = null;

        //协议类型，1--tcp，2--http
        int ptype = protocolType;
        switch (ptype) {
            case 1 -> protocol = NetAddress.Protocol.TCP;
            case 2 -> protocol = NetAddress.Protocol.HTTP;
        }

        //
        NetAddress netAddress = clientInfo.getPortMappingAddress().get(serverPort);
        if (Objects.nonNull(netAddress)) {
            return Result.FAIL("映射端口失败！映射的端口已存在！");
        }

        //添加到映射
        clientInfo.getPortMappingAddress().put(serverPort, new NetAddress(host, port, protocol));

        //判断当前客户端是否在线，在线的话则需要监听新添加的端口
        if (clientInfo.isOnline()) {
            Server.listenLocal(Collections.singleton(serverPort), clientInfo);
        }

        //重新保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("添加映射端口成功！");
    }


    /***
     * 添加映射
     * @param params {"key":"数字去掉双引号", "oldServerPort":"数字去掉双引号", "newServerPort":"数字去掉双引号", "host":"", "port":"数字去掉双引号", "protocol":"1或2 数字去掉双引号"}
     * @return
     */
    public Result modifyClientPortMapping(JsonObject params) {
        Long key = params.getLong("key");
        Integer oldServerPort = params.getInteger("oldServerPort");
        Integer newServerPort = params.getInteger("newServerPort");
        String host = params.getString("host");
        Integer port = params.getInteger("port");

        //协议类型，1--tcp，2--http，3--https(https暂未实现)
        Integer protocolType = params.getInteger("protocol");

        if (Objects.isNull(key)) {
            return Result.FAIL("修改映射端口失败！客户端key为空！");
        }

        if (Objects.isNull(oldServerPort)) {
            return Result.FAIL("修改映射端口失败！服务端映射端口为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("当前客户端key不存在！修改映射端口失败！");
        }

        //老映射地址
        NetAddress oldNetAddress = clientInfo.getPortMappingAddress().get(oldServerPort);

        if (Objects.isNull(oldNetAddress)) {
            return Result.FAIL("修改映射端口失败！修改的服务端映射端口不存在！");
        }

        if (Objects.isNull(newServerPort)) {
            return Result.FAIL("修改映射端口失败！服务端映射端口号为空！");
        }

        if (StringUtil.isNullOrEmpty(host) || Objects.isNull(port)) {
            return Result.FAIL("修改映射端口失败！客户端映射的地址或端口为空！");
        }

        NetAddress netAddress = clientInfo.getPortMappingAddress().get(newServerPort);

        if (Objects.nonNull(netAddress)) {
            return Result.FAIL("修改映射端口失败！服务端映射的新端口号已存在！请更换别的端口！");
        }

        if (Objects.isNull(protocolType)) {
            return Result.FAIL("修改映射端口失败！映射端口协议类型为空！");
        }
        NetAddress.Protocol protocol = null;

        //协议类型，1--tcp，2--http
        int ptype = protocolType;
        switch (ptype) {
            case 1 -> protocol = NetAddress.Protocol.TCP;
            case 2 -> protocol = NetAddress.Protocol.HTTP;
        }

        //监听端口的通道
        Channel channel = clientInfo.getListenPortMap().get(oldServerPort);

        //关闭本地端口
        ChannelFuture channelFuture = closeLocalPort(clientInfo, channel, oldServerPort);
        if (Objects.nonNull(channelFuture)) {
            NetAddress.Protocol finalProtocol = protocol;
            channelFuture.addListener(closeFuture -> {
                //是否关闭该端口成功，成功则需要移除该端口，并添加新端口信息
                if (closeFuture.isSuccess()) {
                    clientInfo.getPortMappingAddress().remove(oldServerPort);
                    //添加新端口信息
                    clientInfo.getPortMappingAddress().put(newServerPort, new NetAddress(host, port, finalProtocol));
                    //判断当前客户端是否在线，在线的话则需要监听新添加的端口
                    if (clientInfo.isOnline()) {
                        Server.listenLocal(Collections.singleton(newServerPort), clientInfo);
                    }
                    //重新保存数据
                    Config.saveTransmitData();
                }
            });
        }
        return Result.SUCCESS("修改映射端口完成！修改后如无法访问，请继续使用原端口！");
    }

    /***
     * 移除映射
     * @param params {"key":"", "serverPort":""}
     * @return
     */
    public Result removeClientPortMapping(JsonObject params) {
        Long key = params.getLong("key");
        Integer serverPort = params.getInteger("serverPort");

        if (Objects.isNull(key)) {
            return Result.FAIL("移除映射端口失败！客户端key为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("当前客户端key不存在！移除映射端口失败！");
        }

        if (Objects.isNull(serverPort)) {
            return Result.FAIL("移除映射端口失败！服务端映射端口为空！");
        }

        //移除映射表
        clientInfo.getPortMappingAddress().remove(serverPort);
        //移除该端口的本地监听channel，如果不为空，则将channel关闭
        Channel channel = clientInfo.getListenPortMap().remove(serverPort);

        //关闭本地端口
        closeLocalPort(clientInfo, channel, serverPort);

        //保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("移除映射端口成功！");
    }

    /***
     * 关闭本地端口 同时关闭本地端口上的连接
     * @param clientInfo
     * @param serverPort
     */
    public ChannelFuture closeLocalPort(ClientInfo clientInfo, Channel serverPortChannel, Integer serverPort) {
        AtomicReference<ChannelFuture> channelFuture = new AtomicReference<>();
        //获取该客户端的远程连接，如果连接为空，则该客户端不在线
        Channel remoteChannel = clientInfo.getRemoteChannel();
        Optional.ofNullable(remoteChannel).ifPresent(remoteCh -> {
            //客户端在线，则获取该客户端上绑定的本地连接通道
            Map<Long, Channel> longChannelMap = remoteCh.attr(Constants.REMOTE_BIND_LOCAL_CHANNEL_KEY).get();
            if (longChannelMap.size() > 0) {
                //本地所有连接通道判断端口号是否和当前移除的端口号一致，一致则需要通知远程关闭该通道对应的连接
                for (Map.Entry<Long, Channel> longChannelEntry : longChannelMap.entrySet()) {
                    Channel localChannel = longChannelEntry.getValue();
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) localChannel.localAddress();
                    if (inetSocketAddress.getPort() == serverPort) {
                        Long tarChannelHash = localChannel.attr(Constants.TAR_CHANNEL_HASH_KEY).get();
                        if (Objects.nonNull(tarChannelHash)) {
                            //通知客户端关闭该端口上的连接通道对应的远程通道
                            DisConnectReqPacks disConnectReqPacks = new DisConnectReqPacks();
                            disConnectReqPacks.setTarChannelHash(tarChannelHash);
                            remoteChannel.writeAndFlush(disConnectReqPacks);
                        }
                    }
                }
            }
        });
        //如果不为空，则将channel关闭
        Optional.ofNullable(serverPortChannel).ifPresent(ch -> {
            channelFuture.set(ch.close());
        });
        return channelFuture.get();
    }


    /***
     * 设置传输服务端口
     * @param params {"serverPort":""}
     * @return
     */
    public Result setTrasmitPort(JsonObject params) {
        Integer port = params.getInteger("serverPort");
        if (Objects.isNull(port) || port < 0 || port > 65535) {
            return Result.FAIL("设置传输服务端口失败！端口号为空或非法！");
        }

        //原端口
        String oldPort = Constants.CONFIG.getProperty(Constants.TRANSMIT_PORT_PROPERTY, Constants.DEF_TRANSMIT_PORT);
        //设置配置
        Constants.CONFIG.setProperty(Constants.TRANSMIT_PORT_PROPERTY, port.toString());
        //Config.saveProperties();
        //关闭监听的传输端口
        Constants.LISTEN_REMOTE_CHANNEL.close().addListener(closeFuture -> {
            if (closeFuture.isSuccess()) {
                log.info("重启传输服务，端口:{}", port);
                //重新监听服务
                Server.listenRemote().addListener(future -> {
                    if (future.isSuccess()) {
                        //保存配置
                        Config.saveProperties();
                    } else {
                        log.info("重启传输服务失败！端口:{}，即将恢复原端口:{}重启..", port, oldPort);
                        //设置配置
                        Constants.CONFIG.setProperty(Constants.TRANSMIT_PORT_PROPERTY, oldPort);
                        //重启原端口
                        Server.listenRemote();
                    }
                });
            }
        });
        return Result.SUCCESS("传输端口已重新设置！");
    }

    /***
     * 设置web服务端口
     * @param params {"webPort":""}
     * @return
     */
    public Result setWebPort(JsonObject params) {
        Integer port = params.getInteger("webPort");
        if (Objects.isNull(port) || port < 0 || port > 65535) {
            return Result.FAIL("设置web服务端口失败！端口号为空或非法！");
        }
        //原端口
        String oldPort = Constants.CONFIG.getProperty(Constants.WEB_PORT_PROPERTY, Constants.WEB_PORT_PROPERTY);
        Constants.FIXED_THREAD_POOL.execute(() -> {
            //设置配置
            Constants.CONFIG.setProperty(Constants.WEB_PORT_PROPERTY, port.toString());
            Constants.VERTX.undeploy(Constants.VERTX_WEB_DEPLOY_ID).onSuccess(han -> {
                log.info("web服务已停止，准备重启web服务，端口:{}", port);
                Constants.VERTX.deployVerticle(new WebManagerVerticle()).onSuccess(deployId -> {
                    Constants.VERTX_WEB_DEPLOY_ID = deployId;
                    //保存配置
                    Config.saveProperties();
                }).onFailure(hann -> {
                    log.error("web服务重启失败！端口:{}，将恢复原端口:{}重启...", port, oldPort, hann);
                    //设置配置
                    Constants.CONFIG.setProperty(Constants.WEB_PORT_PROPERTY, oldPort);
                    //原端口
                    Constants.VERTX.deployVerticle(new WebManagerVerticle()).onSuccess(deployId -> Constants.VERTX_WEB_DEPLOY_ID = deployId);
                });
            });
        });
        return Result.SUCCESS("web端口已重新设置！");
    }

    JsonObject getBodyAsJson(RoutingContext context) {
        JsonObject bodyAsJson = context.getBodyAsJson();
        if (Objects.isNull(bodyAsJson)) {
            bodyAsJson = new JsonObject();
        }
        return bodyAsJson;
    }

}
