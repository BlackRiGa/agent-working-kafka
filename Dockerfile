FROM alpine:3.13
EXPOSE 8080
RUN apk add openjdk11
COPY ./ConsumerKafkaListener/build/libs/ConsumerKafkaListener-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","app.jar"]