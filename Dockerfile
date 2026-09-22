# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-jammy AS builder
WORKDIR /builder
ARG JAR_FILE=tayviscon-app/target/tayviscon-app-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd --system spring \
  && useradd --system --gid spring --no-create-home spring
WORKDIR /application
COPY --from=builder --chown=spring:spring /builder/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /builder/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /builder/extracted/application/ ./
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/ >/dev/null || exit 1
LABEL org.opencontainers.image.source=https://github.com/tayviscon-io/tayviscon-site
ENTRYPOINT ["java", "-jar", "application.jar"]
