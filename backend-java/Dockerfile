FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring
COPY --from=build --chown=spring:spring /workspace/target/tradeguard-backend-*.jar /app/app.jar
COPY --chown=spring:spring entrypoint.sh /app/entrypoint.sh
RUN chmod 0555 /app/entrypoint.sh
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
