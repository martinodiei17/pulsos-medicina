FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN cd sistema-pulsos && ./mvnw clean package -DskipTests || mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/sistema-pulsos/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
