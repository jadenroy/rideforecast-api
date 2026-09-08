FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system rideforecast \
    && useradd --system --gid rideforecast --no-create-home --shell /usr/sbin/nologin rideforecast

COPY --from=build --chown=rideforecast:rideforecast /app/target/*.jar app.jar

USER rideforecast

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
