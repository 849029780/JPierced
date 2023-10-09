FROM openjdk:17
LABEL maintainer=Jian
RUN mkdir jpserver
COPY ./target/server.properties jpserver/
COPY ./target/PServer-1.0-SNAPSHOT.jar jpserver/
WORKDIR jpserver
CMD ["java","-jar","-Xms40M","-Xmx150M","PServer-1.0-SNAPSHOT.jar"]