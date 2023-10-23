FROM openjdk:17
LABEL maintainer=Jian
RUN rm -f /etc/localtime \
&& ln -sv /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
&& echo "Asia/Shanghai" > /etc/timezone
RUN mkdir jpserver
COPY ./target/server.properties jpserver/
COPY ./target/PServer-1.0-SNAPSHOT.jar jpserver/
WORKDIR jpserver
CMD ["java","-jar","-Xms40M","-Xmx500M","PServer-1.0-SNAPSHOT.jar"]