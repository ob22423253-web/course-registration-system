# Multi-stage so the final image only carries the JRE + fat jar, not Maven + sources.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom first so dependency resolution caches independently of source changes.
COPY pom.xml ./
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Runtime ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user — keeps the container from running as root on Render.
RUN groupadd -r app && useradd -r -g app app

COPY --from=build /workspace/target/course-registration-backend-*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080

# Spring reads PORT through application.yml's ${PORT:8080}; nothing extra needed.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
