FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN sh mvnw -B package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app
RUN mkdir /data && chown 10001:10001 /data
COPY --from=build /src/target/*.jar /app/rocketfly.jar
COPY scripts/SetupToken.java /app/SetupToken.java
ENV ROCKETFLY_DATA_DIR=/data
USER 10001:10001
ENTRYPOINT ["java","-jar","/app/rocketfly.jar"]
