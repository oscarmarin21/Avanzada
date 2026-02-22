FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
EXPOSE 9000

COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
