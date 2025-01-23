FROM openjdk:17
LABEL maintainer=Jian
RUN rm -f /etc/localtime \
&& ln -sv /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
&& echo "Asia/Shanghai" > /etc/timezone
RUN mkdir jpclient
COPY ./target/client.yml jpclient/
COPY ./target/PClient-1.0-SNAPSHOT-jar-with-dependencies.jar jpclient/
WORKDIR jpclient
CMD ["java","-Dio.netty.leakDetection.level=paranoid","-Xms40M","-Xmx150M","-jar","PClient-1.0-SNAPSHOT-jar-with-dependencies.jar"]