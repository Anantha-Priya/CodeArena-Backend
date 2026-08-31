# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Maven Wrapper + pom.xml first so dependency resolution is cached in its own layer,
# independent of source changes (mvnw isn't reliably executable after a Windows checkout).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/codearena-0.1.0.jar app.jar

# Informational only - the app actually binds to ${PORT:8080} at runtime (see
# application.properties); Render assigns PORT dynamically regardless of this value.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
