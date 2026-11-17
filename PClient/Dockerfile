FROM eclipse-temurin:21-jre
LABEL maintainer=Jian
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN mkdir -p /jpclient
COPY ./target/client.yml /jpclient/
COPY ./target/PClient-1.0-SNAPSHOT-jar-with-dependencies.jar /jpclient/
WORKDIR /jpclient
CMD ["java","-XX:+UseG1GC","-Xms40M","-Xmx150M","-jar","PClient-1.0-SNAPSHOT-jar-with-dependencies.jar"]