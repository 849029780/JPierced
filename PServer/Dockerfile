FROM eclipse-temurin:21-jre
LABEL maintainer=Jian
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN mkdir -p /jpserver
COPY ./target/server.yml /jpserver/
COPY ./target/PServer-1.0-SNAPSHOT-jar-with-dependencies.jar /jpserver/
WORKDIR /jpserver
CMD ["java","-XX:+UseG1GC","-Xms40M","-Xmx500M","-jar","PServer-1.0-SNAPSHOT-jar-with-dependencies.jar"]