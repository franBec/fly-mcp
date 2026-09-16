# fly-mcp MCP server image: build the Spring Boot jar, run it on a JRE.
# fontconfig + DejaVu give the headless frame renderer a real monospace font.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/libs/fly-mcp-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
