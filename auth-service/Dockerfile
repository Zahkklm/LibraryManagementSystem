FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
COPY --from=build /auth-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
