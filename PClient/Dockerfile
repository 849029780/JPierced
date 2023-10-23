FROM openjdk:17
LABEL maintainer=Jian
RUN rm -f /etc/localtime \
&& ln -sv /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
&& echo "Asia/Shanghai" > /etc/timezone
RUN mkdir jpclient
COPY ./target/client.properties jpclient/
COPY ./target/PClient-1.0-SNAPSHOT.jar jpclient/
WORKDIR jpclient
CMD ["java","-jar","-Xms40M","-Xmx150M","PClient-1.0-SNAPSHOT.jar"]