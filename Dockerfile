FROM maven:3-openjdk-17-slim as build

WORKDIR /app

COPY pom.xml settings.xml ./
COPY src ./src

RUN --mount=type=secret,id=PERSONAL_ACCESS_TOKEN \
   export PERSONAL_ACCESS_TOKEN=$(cat /run/secrets/PERSONAL_ACCESS_TOKEN) && \
   mvn -s settings.xml clean install

FROM openjdk:17
WORKDIR /app
COPY --from=build /app/target/application-rest-service-1.0-SNAPSHOT.jar /app/application-rest-service.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/app/application-rest-service.jar"]

