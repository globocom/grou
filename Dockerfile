FROM maven:3-jdk-8

MAINTAINER tuxmonteiro

ENV VERSION 0.0.1-SNAPSHOT
ENV MONGO_HOST: mongo.local
ENV OS_AUTH_URL: http://controller:5000/v3
ENV OS_PROJECT_DOMAIN_NAME: grou

EXPOSE 8080

COPY . .

RUN apt-get update -y && apt-get install -y make && mvn clean package spring-boot:build-info -DskipTests

CMD make run
