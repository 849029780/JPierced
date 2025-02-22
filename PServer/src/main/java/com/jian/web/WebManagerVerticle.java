package com.jian.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jian.beans.transfer.beans.NetAddr;
import com.jian.beans.transfer.req.DisConnectClientReqPacks;
import com.jian.beans.transfer.req.UdpPortMappingAddReqPacks;
import com.jian.beans.transfer.req.UdpPortMappingRemReqPacks;
import com.jian.commons.Constants;
import com.jian.start.Config;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.NetAddress;
import com.jian.transmit.NetAddressBase;
import com.jian.transmit.tcp.TcpServer;
import com.jian.transmit.udp.UdpClient;
import com.jian.utils.JsonUtils;
import com.jian.web.result.Page;
import com.jian.web.result.Result;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.ssl.SslProtocols;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
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
     * 登录请求
     */
    private final String LOGIN_URI = "/api/login";

    /***
     * 静态页面路径
     */
    private final String STATIC_START_WITH_URI = "/page/";

    /***
     * 跳过所有
     */
    private final Boolean SKIP_ALL = true;


    @Override
    public void start() {
        Integer port = Constants.CONFIG.getWeb().getPort();
        Router router = routerManager();
        if (Constants.IS_ENABLE_HTTPS) {
            HttpServerOptions httpServerOptions = new HttpServerOptions();
            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            pemKeyCertOptions.setCertPath(Config.getCertAbsolutePath()).setKeyPath(Config.getCertKeyAbsolutePath());
            httpServerOptions.setSsl(true).setKeyCertOptions(pemKeyCertOptions);
            httpServerOptions.setSslEngineOptions(new OpenSSLEngineOptions());
            httpServerOptions.addEnabledSecureTransportProtocol(SslProtocols.TLS_v1_3);
            httpServer = vertx.createHttpServer(httpServerOptions);
        } else {
            httpServer = vertx.createHttpServer();
        }
        httpServer.requestHandler(router).listen(port).onSuccess(han -> log.info("web服务启动完成！端口:{}", port)).onFailure(han -> {
            Throwable cause = han.getCause();
            log.error("web服务启动失败！端口:{}，错误原因:", port, cause);
        });
    }

    @Override
    public void stop() {
        httpServer.close();
    }

    public Router routerManager() {
        Router router = Router.router(this.vertx);
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("Access-Control-Allow-Method");
        allowedHeaders.add("Access-Control-Allow-Credentials");
        allowedHeaders.add("Content-Type");

        ResponseContentTypeHandler responseContentTypeHandler = ResponseContentTypeHandler.create();
        //设置响应头Content-type为json
        router.route("/api/*").produces(HttpHeaderValues.APPLICATION_JSON.toString()).handler(responseContentTypeHandler);
        router.route()
                .handler(CorsHandler.create().allowedHeaders(allowedHeaders).allowedMethods(new HashSet<>(HttpMethod.values())))
                .handler(BodyHandler.create())
                .handler(han -> {
                    HttpServerRequest request = han.request();
                    String uri = request.uri();
                    //如果访问路径为登录页面或登录请求，则跳过权限认证(白名单)
                    if (uri.equals(LOGIN_URI) || uri.startsWith(STATIC_START_WITH_URI)) {
                        han.next();
                        return;
                    }


                    //跳过所有认证
                    if (SKIP_ALL) {
                        han.next();
                        return;
                    }


                    //获取请求token
                    String token = request.getHeader("token");
                    //token为空，则跳转到登录页
                    if (StringUtil.isNullOrEmpty(token)) {
                        //需要登录
                        han.response().end(JsonUtils.toJson(Result.UN_LOGIN("请登录后操作！")));
                    } else {
                        //token认证
                        if (checkToken(token)) {
                            //token正确
                            han.next();
                        } else {
                            //需要登录
                            han.response().end(JsonUtils.toJson(Result.UN_LOGIN("请登录后操作！")));
                        }
                    }
                });


        //静态页面
        router.route(HttpMethod.GET, STATIC_START_WITH_URI + "*")
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

        router.route(HttpMethod.POST, "/api/modifyClient").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = modifyClient(params);
            han.response().end(JsonUtils.toJson(result));
        });

        router.route(HttpMethod.POST, "/api/shotClient").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = shotClient(params);
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

        router.route(HttpMethod.POST, "/api/listenPort").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = listenPort(params);
            han.response().end(JsonUtils.toJson(result));
        });

        router.route(HttpMethod.POST, "/api/closePort").handler(han -> {
            JsonObject params = getBodyAsJson(han);
            Result result = closePort(params);
            han.response().end(JsonUtils.toJson(result));
        });
        return router;
    }


    /***
     * 校验token
     * @param token 登录token
     * @return 响应结果
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
     * @return 响应结果 token
     */
    public Result login(JsonObject params) {
        String username = params.getString("username", "");
        String pwd = params.getString("pwd", "");
        String sysUserName = Constants.CONFIG.getWeb().getDefUsername();
        String sysPwd = Constants.CONFIG.getWeb().getDefPassword();
        Result result;
        //
        if (sysUserName.equals(username) && sysPwd.equals(pwd)) {
            LocalDateTime localDateTime = LocalDateTime.now().plusDays(7L);
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
     * @return 响应结果
     */
    public Result clienList(JsonObject params) {
        Long key = params.getLong("key");
        String name = params.getString("name");
        Integer serverPort = params.getInteger("serverPort");

        Collection<ClientInfo> values = Constants.CLIENTS.values();

        List<ClientInfo> collectClient = values.stream().filter(cli -> {
            boolean isEq = true;
            if (Objects.nonNull(key)) {
                isEq = cli.getKey().equals(key);
            }

            if (isEq && !StringUtil.isNullOrEmpty(name)) {
                isEq = cli.getName().equals(name);
            }

            if (isEq && Objects.nonNull(serverPort)) {
                Map<Integer, NetAddress> portMappingAddress = cli.getPortMappingAddress();
                NetAddress netAddress = portMappingAddress.get(serverPort);
                isEq = Objects.nonNull(netAddress);
            }
            return isEq;
        }).collect(Collectors.toList());

        return Result.SUCCESS(Page.page(collectClient));
    }

    /***
     * 添加客户端
     * @param params {"key":"数字去掉双引号", "pwd":"xxx", "name":""}
     * @return 响应结果
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
     * 修改客户端
     * @param params {"key":"数字去掉双引号","newKey":新的key, "pwd":"xxx", "name":""}
     * @return 响应结果
     */
    private Result modifyClient(JsonObject params) {
        Long key = params.getLong("key");
        Long newKey = params.getLong("newKey");
        String pwd = params.getString("pwd");
        String name = params.getString("name");

        if (Objects.isNull(key)) {
            return Result.FAIL("修改客户端失败！key为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);

        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("修改客户端失败！客户端key不存在！");
        }

        if (Objects.nonNull(newKey)) {
            clientInfo.setKey(newKey);
        }
        if (!StringUtil.isNullOrEmpty(pwd)) {
            clientInfo.setPwd(pwd);
        }
        if (!StringUtil.isNullOrEmpty(name)) {
            clientInfo.setName(name);
        }

        //重新保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("修改客户端信息成功！");
    }

    /***
     * 移除客户端
     * @param params {"key":"数字去掉双引号"}
     * @return 响应结果
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
     * 踢出客户端
     * @param params {"key":"数字去掉双引号"}
     * @return 响应结果
     */
    private Result shotClient(JsonObject params) {
        Long key = params.getLong("key");
        if (Objects.isNull(key)) {
            return Result.FAIL("踢出客户端失败！key为空！");
        }
        ClientInfo clientInfo = Constants.CLIENTS.get(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("踢出客户端失败！当前客户端key不存在！");
        }

        Channel remoteChannel = clientInfo.getRemoteChannel();
        if (Objects.isNull(remoteChannel)) {
            return Result.FAIL("踢出客户端失败！当前客户端key未连接！");
        }
        DisConnectClientReqPacks disConnectClientReqPacks = new DisConnectClientReqPacks();
        disConnectClientReqPacks.setCode((byte) 1);
        remoteChannel.writeAndFlush(disConnectClientReqPacks);
        return Result.FAIL("客户端已下线！");
    }


    /***
     * 添加映射
     * @param params {"key":"数字去掉双引号", "serverPort":"xxx", "host":"", "port":"数字去掉双引号", "protocol":"1或2 数字去掉双引号", "cliUseHttps": true}
     * @return 响应结果
     */
    public Result addClientPortMapping(JsonObject params) {
        Long key = params.getLong("key");
        Integer serverPort = params.getInteger("serverPort");
        String host = params.getString("host");
        Integer port = params.getInteger("port");
        //协议类型，1--tcp，2--http，3--https，4--udp
        Integer protocolType = params.getInteger("protocol");
        //客户端是否使用Https
        Boolean cliUseHttps = params.getBoolean("cliUseHttps", false);

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
        NetAddress.Protocol protocol = getProtocolByType(protocolType);

        if (!Constants.IS_ENABLE_HTTPS && protocol == NetAddress.Protocol.HTTPS) {
            return Result.FAIL("映射端口失败！Https证书未配置，不允许使用Https！");
        }
        //
        NetAddress netAddress = clientInfo.getPortMappingAddress().get(serverPort);
        if (Objects.nonNull(netAddress)) {
            return Result.FAIL("映射端口失败！映射的端口已存在！");
        }
        //添加到映射map
        clientInfo.getPortMappingAddress().put(serverPort, new NetAddress(host, port, protocol, cliUseHttps));
        //判断当前客户端是否在线，在线的话则需要监听新添加的端口
        if (clientInfo.isOnline()) {
            if (protocol == NetAddress.Protocol.UDP) {

                NetAddr netAddr = new NetAddr(serverPort, host, port);
                UdpPortMappingAddReqPacks udpPortMappingAddReqPacks = new UdpPortMappingAddReqPacks();
                udpPortMappingAddReqPacks.setNetAddrList(Collections.singletonList(netAddr));

                Channel ackChannel = clientInfo.getAckChannel();
                ackChannel.writeAndFlush(udpPortMappingAddReqPacks).addListener(future -> {
                    if (future.isSuccess()) {
                        UdpClient.listenLocal(Collections.singleton(serverPort), clientInfo);
                    }
                });
            } else {
                TcpServer.listenLocal(Collections.singleton(serverPort), clientInfo);
            }
        }

        //重新保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("添加映射端口成功！");
    }


    /***
     * 修改映射
     * @param params {"key":"数字去掉双引号", "oldServerPort":"数字去掉双引号", "newServerPort":"数字去掉双引号", "host":"", "port":"数字去掉双引号", "protocol":"1或2 数字去掉双引号"}
     * @return 响应结果 响应结果
     */
    public Result modifyClientPortMapping(JsonObject params) {
        Long key = params.getLong("key");
        Integer oldServerPort = params.getInteger("oldServerPort");
        Integer newServerPort = params.getInteger("newServerPort");
        String host = params.getString("host");
        Integer port = params.getInteger("port");
        //客户端是否使用Https
        Boolean cliUseHttps = params.getBoolean("cliUseHttps");

        //协议类型，1--tcp，2--http，3--https
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

        if (Objects.isNull(cliUseHttps)) {
            cliUseHttps = oldNetAddress.getCliUseHttps();
        }

        if (Objects.isNull(newServerPort)) {
            return Result.FAIL("修改映射端口失败！服务端映射端口号为空！");
        }

        if (StringUtil.isNullOrEmpty(host) || Objects.isNull(port)) {
            return Result.FAIL("修改映射端口失败！客户端映射的地址或端口为空！");
        }

        if (Objects.isNull(protocolType)) {
            return Result.FAIL("修改映射端口失败！映射端口协议类型为空！");
        }
        NetAddress.Protocol protocol = getProtocolByType(protocolType);

        if (!Constants.IS_ENABLE_HTTPS && protocol == NetAddress.Protocol.HTTPS) {
            return Result.FAIL("修改映射端口失败！Https证书未配置，不允许使用Https！");
        }

        //判断是否在线，在线则需要关闭端口重新监听新端口
        if (clientInfo.isOnline()) {
            //监听端口的通道
            Channel channel = clientInfo.getListenPortMap().get(oldServerPort);

            //判断原端口是udp还是tcp
            if (oldNetAddress.getProtocol() == NetAddressBase.Protocol.UDP) {

                UdpPortMappingRemReqPacks udpPortMappingRemReqPacks = new UdpPortMappingRemReqPacks();
                udpPortMappingRemReqPacks.setSourcePort(oldServerPort);
                Channel ackChannel = clientInfo.getAckChannel();
                //移除客户端udp映射
                ackChannel.writeAndFlush(udpPortMappingRemReqPacks);

                ChannelFuture channelFuture = UdpClient.closeLocalPortFuture(channel);
                channelFuture.addListener(future -> {
                    if (clientInfo.isOnline()) {
                        if (protocol == NetAddress.Protocol.UDP) {
                            NetAddr netAddr = new NetAddr(newServerPort, host, port);
                            UdpPortMappingAddReqPacks udpPortMappingAddReqPacks = new UdpPortMappingAddReqPacks();
                            udpPortMappingAddReqPacks.setNetAddrList(Collections.singletonList(netAddr));
                            ackChannel.writeAndFlush(udpPortMappingAddReqPacks).addListener(future1 -> {
                                if (future1.isSuccess()) {
                                    UdpClient.listenLocal(Set.of(newServerPort), clientInfo);
                                }
                            });
                        } else {
                            TcpServer.listenLocal(Set.of(newServerPort), clientInfo);
                        }
                    }
                    //重新保存数据
                    Config.saveTransmitData();
                });
            } else {
                //关闭本地端口
                ChannelFuture channelFuture = TcpServer.closeLocalPort(clientInfo, channel, oldServerPort);
                if (Objects.nonNull(channelFuture)) {
                    Boolean finalCliUseHttps = cliUseHttps;
                    channelFuture.addListener(closeFuture -> {
                        //是否关闭该端口成功，成功则需要移除该端口，并添加新端口信息
                        if (closeFuture.isSuccess()) {
                            clientInfo.getPortMappingAddress().remove(oldServerPort);
                            clientInfo.getListenPortMap().remove(oldServerPort);
                            //添加新端口信息
                            clientInfo.getPortMappingAddress().put(newServerPort, new NetAddress(host, port, protocol, finalCliUseHttps));
                            //判断当前客户端是否在线，在线的话则需要监听新添加的端口
                            if (clientInfo.isOnline()) {
                                if (protocol == NetAddress.Protocol.UDP) {
                                    NetAddr netAddr = new NetAddr(newServerPort, host, port);
                                    UdpPortMappingAddReqPacks udpPortMappingAddReqPacks = new UdpPortMappingAddReqPacks();
                                    udpPortMappingAddReqPacks.setNetAddrList(Collections.singletonList(netAddr));
                                    Channel ackChannel = clientInfo.getAckChannel();
                                    ackChannel.writeAndFlush(udpPortMappingAddReqPacks).addListener(future1 -> {
                                        if (future1.isSuccess()) {
                                            UdpClient.listenLocal(Set.of(newServerPort), clientInfo);
                                        }
                                    });
                                } else {
                                    TcpServer.listenLocal(Set.of(newServerPort), clientInfo);
                                }
                            }
                            //重新保存数据
                            Config.saveTransmitData();
                        }
                    });
                } else {
                    clientInfo.getPortMappingAddress().remove(oldServerPort);
                    oldNetAddress.setHost(host);
                    oldNetAddress.setPort(port);
                    oldNetAddress.setProtocol(protocol);
                    //添加新端口信息
                    clientInfo.getPortMappingAddress().put(newServerPort, oldNetAddress);
                    //重新保存数据
                    Config.saveTransmitData();
                }
            }
        } else {
            clientInfo.getPortMappingAddress().remove(oldServerPort);
            oldNetAddress.setHost(host);
            oldNetAddress.setPort(port);
            oldNetAddress.setProtocol(protocol);
            oldNetAddress.setCliUseHttps(cliUseHttps);
            //添加新端口信息
            clientInfo.getPortMappingAddress().put(newServerPort, oldNetAddress);
            //重新保存数据
            Config.saveTransmitData();
        }
        return Result.SUCCESS("修改映射端口完成！修改后如无法访问，请继续使用原端口！");
    }

    /***
     * 关闭端口
     * @param params {"key":"数字去掉双引号","port":""}
     * @return 响应结果
     */
    private Result closePort(JsonObject params) {
        Long key = params.getLong("key");
        Integer port = params.getInteger("port");
        if (Objects.isNull(key)) {
            return Result.FAIL("关闭监听的端口失败！客户端key为空！");
        }
        if (Objects.isNull(port)) {
            return Result.FAIL("关闭监听的端口失败！端口号为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("当前客户端key不存在！关闭端口失败！");
        }
        NetAddress netAddress = clientInfo.getPortMappingAddress().get(port);
        if (Objects.isNull(netAddress)) {
            return Result.FAIL("当前客户端未添加映射该端口！关闭端口失败！");
        }
        Channel channel = clientInfo.getListenPortMap().get(port);
        if (Objects.isNull(channel)) {
            return Result.FAIL("当前客户端未映射该端口！关闭端口失败！");
        }

        NetAddressBase.Protocol protocol = netAddress.getProtocol();
        if (protocol == NetAddressBase.Protocol.UDP) {
            UdpClient.closeLocalPort(channel);
        } else {
            //关闭端口
            ChannelFuture channelFuture = TcpServer.closeLocalPort(clientInfo, channel, port);
            if (Objects.nonNull(channelFuture)) {
                channelFuture.addListener(closeFuture -> {
                    //是否关闭该端口成功，成功则需要移除该端口，并添加新端口信息
                    if (closeFuture.isSuccess()) {
                        clientInfo.getListenPortMap().remove(port);
                    }
                });
            }
        }
        return Result.SUCCESS("当前客户端映射的端口已关闭！");
    }

    /***
     * 监听端口
     * @param params {"key":"数字去掉双引号","port":""}
     * @return 响应结果
     */
    private Result listenPort(JsonObject params) {
        Long key = params.getLong("key");
        Integer port = params.getInteger("port");
        if (Objects.isNull(key)) {
            return Result.FAIL("监听端口失败！客户端key为空！");
        }
        if (Objects.isNull(port)) {
            return Result.FAIL("监听端口失败！端口号为空！");
        }

        ClientInfo clientInfo = Constants.CLIENTS.get(key);
        if (Objects.isNull(clientInfo)) {
            return Result.FAIL("当前客户端key不存在！监听端口失败！");
        }
        if (!clientInfo.isOnline()) {
            return Result.FAIL("当前客户端不在线！不允许监听端口！");
        }
        NetAddress netAddress = clientInfo.getPortMappingAddress().get(port);
        if (Objects.isNull(netAddress)) {
            return Result.FAIL("当前客户端未添加映射该端口！监听端口失败！");
        }
        Channel channel = clientInfo.getListenPortMap().get(port);
        if (Objects.nonNull(channel)) {
            return Result.FAIL("当前客户端已监听该端口！不可重复监听！");
        }
        //监听端口
        if (netAddress.getProtocol() == NetAddressBase.Protocol.UDP) {
            UdpClient.listenLocal(Set.of(port), clientInfo);
        } else {
            TcpServer.listenLocal(Set.of(port), clientInfo);
        }
        return Result.SUCCESS("当前客户端已监听该端口！");
    }

    /***
     *
     *
     * @param protocolType 1--tcp 2--http 3--https 4--udp
     * @return 响应结果
     */
    private NetAddress.Protocol getProtocolByType(int protocolType) {
        NetAddress.Protocol protocol;
        switch (protocolType) {
            case 2 -> protocol = NetAddress.Protocol.HTTP;
            case 3 -> protocol = NetAddress.Protocol.HTTPS;
            case 4 -> protocol = NetAddress.Protocol.UDP;
            default -> protocol = NetAddress.Protocol.TCP;
        }
        return protocol;
    }

    /***
     * 移除映射
     * @param params {"key":"", "serverPort":""}
     * @return 响应结果
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


        NetAddress netAddress = clientInfo.getPortMappingAddress().get(serverPort);

        //移除该端口的本地监听channel，如果不为空，则将channel关闭
        Channel channel = clientInfo.getListenPortMap().remove(serverPort);

        if (netAddress.getProtocol() == NetAddressBase.Protocol.UDP) {
            UdpPortMappingRemReqPacks udpPortMappingRemReqPacks = new UdpPortMappingRemReqPacks();
            udpPortMappingRemReqPacks.setSourcePort(serverPort);
            Channel ackChannel = clientInfo.getAckChannel();
            ackChannel.writeAndFlush(udpPortMappingRemReqPacks).addListener(future -> {
                if (future.isSuccess()) {
                    UdpClient.closeLocalPort(channel);
                }
            });
        } else {
            //关闭本地端口
            TcpServer.closeLocalPort(clientInfo, channel, serverPort);
        }

        //保存数据
        Config.saveTransmitData();
        return Result.SUCCESS("移除映射端口成功！");
    }


    /***
     * 设置传输服务端口
     * @param params {"serverPort":""}
     * @return 响应结果
     */
    public Result setTrasmitPort(JsonObject params) {
        Integer port = params.getInteger("serverPort");
        if (Objects.isNull(port) || port < 0 || port > 65535) {
            return Result.FAIL("设置传输服务端口失败！端口号为空或非法！");
        }

        //原端口
        Integer oldPort = Constants.CONFIG.getTransmit().getPort();
        //设置配置
        Constants.CONFIG.getTransmit().setPort(port);
        //Config.saveProperties();
        //关闭监听的传输端口
        Constants.LISTEN_REMOTE_CHANNEL.close().addListener(closeFuture -> {
            if (closeFuture.isSuccess()) {
                log.info("重启传输服务，端口:{}", port);
                //重新监听服务
                TcpServer.listenRemote().addListener(future -> {
                    if (future.isSuccess()) {
                        //保存配置
                        Config.saveProperties();
                    } else {
                        log.warn("重启传输服务失败！端口:{}，即将恢复原端口:{}重启..", port, oldPort);
                        //设置配置
                        Constants.CONFIG.getTransmit().setPort(oldPort);
                        //重启原端口
                        TcpServer.listenRemote();
                    }
                });
            }
        });
        return Result.SUCCESS("传输端口已重新设置！");
    }

    /***
     * 设置web服务端口
     * @param params {"webPort":""}
     * @return 响应结果
     */
    public Result setWebPort(JsonObject params) {
        Integer port = params.getInteger("webPort");
        if (Objects.isNull(port) || port < 0 || port > 65535) {
            return Result.FAIL("设置web服务端口失败！端口号为空或非法！");
        }
        //原端口
        Integer oldPort = Constants.CONFIG.getWeb().getPort();
        Constants.FIXED_THREAD_POOL.execute(() -> {
            //设置配置
            Constants.CONFIG.getWeb().setPort(port);
            Constants.VERTX.undeploy(Constants.VERTX_WEB_DEPLOY_ID).onSuccess(han -> {
                log.info("web服务已停止，准备重启web服务，端口:{}", port);
                Constants.VERTX.deployVerticle(new WebManagerVerticle()).onSuccess(deployId -> {
                    Constants.VERTX_WEB_DEPLOY_ID = deployId;
                    //保存配置
                    Config.saveProperties();
                }).onFailure(hann -> {
                    log.error("web服务重启失败！端口:{}，将恢复原端口:{}重启...", port, oldPort, hann);
                    //设置配置
                    Constants.CONFIG.getWeb().setPort(oldPort);
                    //原端口
                    Constants.VERTX.deployVerticle(new WebManagerVerticle()).onSuccess(deployId -> Constants.VERTX_WEB_DEPLOY_ID = deployId);
                });
            });
        });
        return Result.SUCCESS("web端口已重新设置！");
    }

    JsonObject getBodyAsJson(RoutingContext context) {
        JsonObject bodyAsJson = context.body().asJsonObject();
        if (Objects.isNull(bodyAsJson)) {
            bodyAsJson = new JsonObject();
        }
        return bodyAsJson;
    }

}
