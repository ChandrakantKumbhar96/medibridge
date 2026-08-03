FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl \
 && addgroup -S medibridge && adduser -S medibridge -G medibridge
COPY --from=build /src/target/medibridge-backend-*.jar app.jar
RUN mkdir -p /app/storage/reports && chown -R medibridge:medibridge /app
USER medibridge

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
