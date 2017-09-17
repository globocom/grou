FROM maven:3-jdk-8

MAINTAINER tuxmonteiro

ENV VERSION 0.0.1-SNAPSHOT
ENV MONGO_HOST: mongo.local
ENV KEYSTONE_URL: http://controller:5000/v3
ENV KEYSTONE_DOMAIN_CONTEXT: grou

EXPOSE 8080

COPY . .

RUN apt-get update -y && apt-get install -y make && mvn clean package spring-boot:build-info -DskipTests

CMD make run
