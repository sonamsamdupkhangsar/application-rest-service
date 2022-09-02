FROM maven:3-openjdk-17-slim as build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN ["mvn", "clean", "install"]

FROM openjdk:17
WORKDIR /app
COPY --from=build /app/target/application-rest-service-1.0-SNAPSHOT.jar /app/application-rest-service.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/app/application-rest-service.jar"]

