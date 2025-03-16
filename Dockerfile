# Start with Maven image to build
FROM maven:3.8.7-eclipse-temurin-17 AS builder
WORKDIR /fileserver
COPY pom.xml .
# RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Then minimal JDK image to run
FROM eclipse-temurin:17-jre
WORKDIR /fileserver
COPY --from=builder /fileserver/target/demo-0.0.1-SNAPSHOT.jar fileserver.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","fileserver.jar"]