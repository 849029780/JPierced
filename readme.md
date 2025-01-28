# 简介

纯Java和Netty写的STUN内网穿透应用，高性能低延迟，已稳定使用近3年；  
(另还有P2P的TURN模式穿透方案，基于UDP实现打洞，延迟会更低些，例如市面上的向日葵等远程桌面软件都是基于该模式，如果TURN不支持，则如同本项目使用的STUN模式一样，但目前没有空再写，有空再说啦..)  
1、支持TCP,UDP,HTTP,HTTPS协议，类似Frp，对比Frp，服务端使用http接口动态管理穿透客户端和动态映射端口（由于前端好久没写了，所以未写前端页面，只有接口，后续可能会增加），具体操作api说明如下；  
2、被穿透客户端与服务器之间使用多路通道复用，减少产生新的连接时重复连接，在网络环境较差的情况也能稳定使用；  
3、穿透的客户端和服务端采用TLSv1.3/SSL双向认证加密通信保证数据传输的安全，使用Openssl的分支Google Boring实现（目前默认关闭SSL，需要的话手动更改配置开启，并配置相应证书即可）；  
4、更好的支持http及https协议，支持反向代理客户端 https->http或http->https的形式，在别的类似穿透应用所谓支持http及https协议，实际上都只是做了TCP报文转发，而未对http协议中Header的Host及Referer,Location中的URL地址及端口进行修改，导致请求部分较严格的http服务时服务验证请求地址与报文中Host地址及端口不一致导致请求被拒绝，以及http服务响应3xx重定向时地址未进行处理，导致跳转到真实地址的页面出现跨域等错误，对此服务端对Host，Referer，Location等http Header中的URL及端口会进行相应修改替换，和Nginx的代理功能类似。  
5、不同操作系统采用不同的Channel实现以使在该操作系统上达到最佳性能，Linux优先使用Io_uring，其次是Epoll，Mac使用Kqueue，Windows采用NIO(具体各系统机制区别可自行了解)，零copy模式实现通道数据处理，大幅提升性能和内存使用率。  
6、一整套的连接管理去除废弃连接，提升性能，包括用户和服务端连接关闭，告知穿透本地和目标地址的连接关闭，服务端端口关闭，关闭所有穿透客户端所有相关连接等。

## 部署和构建
1、需要一台具备公网IP的服务器，以及需要穿透的内网客户机，将PServer服务部署至公网服务器中运行，被穿透客户机运行PClient服务，可使用api接口在服务端管理客户端及动态添加端口映射等。
2、拉取最新分支的代码到本地，由于基于JDK17编写，请确保安装的JDK版本在17或以上，可根据情况调整项目resources下的logback.xml日志级别等配置。  
由于项目中默认放置了我自己生成的CA证书及密钥，可以直接使用，如需要自行生成，生成后替换掉resources/certs中相应的证书文件即可，注意生成的客户端及服务端证书都必须是统一CA签名的(目前默认传输关闭了SSL，使用Netty4.2后相关证书域名信息必须符合，否则会连接异常)。  
使用Maven命令mvn package编译打包成可运行的jar包(打包后在项目目录的target目录下)；  
3、将PServer.jar上传到有公网的服务器上，并同时上传server.yml配置文件(配置说明如上)  
然后使用```jar -jar PServer.jar```命令运行即可，或使用nohup命令在后台运行```nohup java -jar PServer-1.0-SNAPSHOT.jar > /dev/null 2>&1 &```  
4、将PClient.jar放到被穿透的机器上，在同目录下放置client.yml配置文件(配置说明如上)  
然后使用```jar -jar PClient.jar```命令运行即可，或使用nohup命令在后台运行```nohup java -jar PClient-1.0-SNAPSHOT.jar > /dev/null 2>&1 &```   
5、使用docker部署PServer，配置参考项目中的Dockerfile，为使达到更佳性能，网桥配置建议使用宿主机端口。   
6、使用docker部署PClient，配置参考项目中的Dockerfile，如果连接宿主机，IP需要填写host.docker.internal，否则会导致连接不上。  
7、【注意】：客户端连接前必须现在服务端上进行对客户端用户和密码添加，然后启动客户端 客户端将与服务进行连接，连接及认证完成后才可使用，根据使用场景设置Jvm的内存大小。


## PServer配置

项目resources路径下server.yml文件是服务端相关配置，内容及说明如下：

```
transmit:
  # 客户端和服务端的数据传输端口
  port: 9998
  # 是否启用ssl加密传输，启用后需要配置相关证书
  use-ssl: false
  # ack通道的端口(该端口用于建立连接和开关闭自动读等操作，netty默认自动读，当发生大文件传输时网络较拥堵时，超出缓冲水位，需要关闭自动读，否则大量数据写入会发生OOM，具体的水位控制可自行了解)
  ack-port: 6211

web:
  # web管理的登录用户名及密码
  def-username: xxxx
  def-password: xxxx
  # web管理的服务端口
  port: 8000
  # 是否启用支持HTTPS网页的穿透，开启支持需要在运行环境的同目录放置https的证书文件pierced.crt，pierced.key，key使用pkcs8，
  # 可使用openssl命令直接将Nginx证书的key转为pkcs8的key，如下命令：
  # openssl pkcs8 -topk8 -inform pem -in 证书.key -outform pem -nocrypt -out 证书pkcs8.key
  # 未配置证书时，启动将会默认关闭使用HTTPS。
  # 配置证书后并启用https后，管理相关接口也将使用https
  use-https: false
```

## PClient配置

项目resources路径下client.yml文件是客户端相关配置，内容及说明如下：

```
server:
  # 服务端域名或IP地址
  host: localhost
  # 服务端传输端口，非ack通道端口
  port: 9998
  # 服务端添加的客户用户名和密码，服务端必须添加客户端用户后客户端才可连接，否则连接不上
  username: 3333
  password: xxx
  # 开启ssl加密，开启后需配置ssl证书
  use-ssl: false 
```



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

修改客户端信息
```
POST请求
Json参数：{"key":客户端数字key,"newKey":新的key, "pwd":"新密码", "name":"客户端名称"}
http://xxx:port/api/modifyClient
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
Json参数：{"key":数字客户端key, "serverPort":映射在服务端上的端口, "host":"被穿透机器上到目标机器的host", "port":被穿透机器上到目标机器的端口号, "protocol":数字，取值如上协议类型说明, "cliUseHttps": true 布尔值}
http://xxx:port/api/addClientPortMapping
```

修改客户端端口映射
```
POST请求  
protocol参数值说明: 1--TCP,2--HTTP,3--HTTPS
Json参数：{"key":数字客户端key, "oldServerPort":原端口, "newServerPort":新端口, "host":"被穿透机器上到目标机器的host", "port":被穿透机器上到目标机器的端口号, "protocol":数字，取值如上协议类型说明, "cliUseHttps": true 布尔值}
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




