# 简介

使用NIO框架Netty写的内网穿透(PServer(服务端)，PClient(被穿透客户端))，Vert.x实现的web服务；  
1、支持TCP,HTTP,HTTPS协议,(暂不支持UDP，后续将会支持)，类似Frp，但在对比Frp上，提供了http接口进行管理客户端比Frp更方便（由于前端好久没写了，所以未写前端页面，只有接口，后续可能会增加），增加删除修改端口映射，关闭指定端口监听，及启用指定端口及踢出客户端下线等都可使用接口操作，客户端无需重启及做别的操作；  
2、使用多路通道复用，减少请求端发起连接时每次都需要被穿透的客户端创建新的连接导致连接请求延迟，比起Frp连接响应更快；  
3、穿透的客户端和服务端采用TLSv1.3/SSL双向认证加密通信保证数据传输的安全，加密套件使用Openssl的分支Google Boring实现，比起JdkSSL性能上更好；  
4、更好的支持HTTP及HTTPS，在别的类似穿透应用所谓支持HTTP及HTTPS协议，实际上都只是做了TCP报文转发，而未对HTTP协议中Header的Host及Referer,Location中的URL地址及端口进行修改，导致请求部分较严格的HTTP服务时服务验证请求地址与报文中Host地址及端口不一致导致请求被拒绝，以及HTTP服务响应3xx重定向时地址未进行处理，导致跳转到真实地址的页面出现跨域等错误，对此服务端对Host，Referer，Location等HTTP Header中的URL及端口会进行相应修改替换，和Nginx的代理功能类似。  
5、不同操作系统采用不同的Channel实现以使在该操作系统上达到最佳性能，Linux默认使用Epoll，Mac使用Kqueue，Windows采用Selector，使用ByteBuf零copy减少数据传输的堆外内存频繁复制到堆内存导致影响处理速度。

## PServer配置

项目resources路径下server.properties文件是服务端相关配置，内容及说明如下：

```
# web管理的服务端口
web.port=8000 
# 是否启用支持HTTPS网页的穿透，开启支持需要在运行环境的同目录放置https的证书文件pierced.crt，pierced.key，key使用pkcs8，未配置启动将会默认关闭HTTPS
# 启用https后，管理相关接口也将使用https
enable.https=true
# web管理的登录用户名及密码
login.username=xxxx
login.pwd=xxxx
# ack通道的端口(ack通道主要用于收发连接及连接响应消息，以及设置被穿透客户端的本地连接上的是否自动读等作用)
ack.port=6210
# 主要的数据传输端口
transmit.port=9999
```

## PClient配置

项目resources路径下cient.properties文件是客户端相关配置，内容及说明如下：

```
# 以上配置的服务端地址
server.host=localhost
# 以上配置的服务端传输端口，注意是传输端口，非ack的端口
server.port=9999
# 客户端登录认证用户名及密码，用户名只支持Long的数字(必须在管理端添加客户端后才能登录，否则连接不上)
key=2222
pwd=xxx
```

## 部署和构建

使用JDK17编写，请确保安装的JDK版本在17或以上，可根据情况调整项目resources下的logback.xml日志级别等配置。  
由于项目中默认放置了我自己生成的CA证书及密钥，可以直接使用，如需要自行生成，生成后替换掉resources/certs中相应的证书文件即可，注意生成的客户端及服务端证书都必须是统一CA签名的。  
使用Maven命令mvn package编译打包成可运行的jar包(打包后在项目目录的target目录下)；  
1、将PServer.jar上传到有公网的服务器上，并同时上传server.properties配置文件(配置说明如上)  
然后使用```jar -jar PServer.jar```命令运行即可，或使用nohup命令在后台运行```java -jar PServer-1.0-SNAPSHOT.jar > /dev/null &2>1```  
2、将PClient.jar放到被穿透的机器上，在同目录下放置client.properties配置文件(配置说明如上)  
然后使用```jar -jar PClient.jar```命令运行即可，或使用nohup命令在后台运行```java -jar PClient-1.0-SNAPSHOT.jar > /dev/null &2>1```  
【注意】：客户端连接前必须现在服务端上进行对客户端用户和密码添加，然后启动客户端 客户端将与服务进行连接，连接及认证完成后才可使用，根据使用场景设置Jvm的内存大小。

## api接口及参数说明

【注意】：请求参数json中的数字参数不能使用引号，如111，使用"111"这样访问将会错误，需去掉引号数字参数的引号。  
登录接口
```
POST请求
Json参数：{"username":"配置的用户名", "pwd":"配置的密码"}
http://xxx:port/api/login
登录成功后响应token，以下的所有请求接口中的header中都必须携带该token才能进行操作。
```

查询在线客户端列表
```
GET请求
Json参数可选：{"key": 数字key, "name":"可选参数", "serverPort":数字}
http://xxx:port/api/clientList
```

添加客户端用户名及密码
```
POST请求
Json参数：{"key":不重复的数字key, "pwd":"密码", "name":"客户端名"}
http://xxx:port/api/addClient
```

移除客户端

```
POST请求
Json参数：{"key":数字key}
http://xxx:port/api/removeClient
```

客户端下线
```
POST请求
Json参数：{"key":数字key}
http://xxx:port/api/shotClient
```

添加客户端的端口映射
```
POST请求  
protocol参数值说明: 1--TCP,2--HTTP,3--HTTPS
Json参数：{"key":数字客户端key, "serverPort":映射在服务端上的端口, "host":"被穿透机器上到目标机器的host", "port":被穿透机器上到目标机器的端口号, "protocol":数字，取值如上协议类型说明}
http://xxx:port/api/addClientPortMapping
```

修改客户端端口映射
```
POST请求  
protocol参数值说明: 1--TCP,2--HTTP,3--HTTPS
Json参数：{"key":数字客户端key, "oldServerPort":原端口, "newServerPort":新端口, "host":"被穿透机器上到目标机器的host", "port":被穿透机器上到目标机器的端口号, "protocol":数字，取值如上协议类型说明}
http://xxx:port/api/modifyClientPortMapping
```

移除客户端端口映射
```
POST请求  
Json参数：{"key":数字客户端key, "serverPort":要移除的端口}
http://xxx:port/api/removeClientPortMapping
```

重设置传输服务端口(重设置后将断开所有客户端，且老的客户端不可连接)
```
POST请求  
Json参数：{"serverPort":}
http://xxx:port/api/setTrasmitPort
```
重新设置web服务端口(重新设置后将重启管理接口的web服务)
```
POST请求  
Json参数：{"webPort":数字端口}
http://xxx:port/api/setWebPort
```
监听指定映射客户端的端口
```
POST请求  
Json参数：{"key":数字，客户端key,"port":已添加的端口号}
http://xxx:port/api/listenPort
```

取消监听指定映射客户端的端口
```
POST请求  
Json参数：{"key":数字，客户端key,"port":端口}
http://xxx:port/api/closePort
```




