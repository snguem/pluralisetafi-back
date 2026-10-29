# Backend pluralisEtatFin + LibreOffice (headless) in a single image.
#
# LibreOffice is only needed by JODConverter: recalculating the formulas of an état financier
# and converting it to PDF. Installing it here keeps it off the host.

# ---- build ---------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# dependencies first, so they stay cached until the pom changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime -------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# libreoffice-calc: xlsx -> pdf and xlsx recalculation. The fonts keep the layout of the models
# (Arial / Calibri metrics) when LibreOffice renders them.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice-calc \
        fonts-liberation fonts-dejavu-core fonts-crosextra-carlito fonts-crosextra-caladea \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --uid 10001 app
WORKDIR /app

# the application reads and writes ./public (models, generated excels, temp files) and ./logs
RUN mkdir -p /app/public /app/logs && chown -R app:app /app
VOLUME /app/public

COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar

USER app
EXPOSE 3302

# prod profile: LibreOffice is in /usr/lib/libreoffice (see application-prod.yml)
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
