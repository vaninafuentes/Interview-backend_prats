# ==============================================================================
# DOCKERFILE MULTIETAPA — INTERVIEW BACKEND (SPRING BOOT)
# ==============================================================================
# Etapa 1: Builder (Compilación limpia, dependencias en caché y generación del JAR)
# Etapa 2: Production (Runtime Distroless mínimo, usuario no-root, trazabilidad OCI)
# ==============================================================================

# ------------------------------------------------------------------------------
# ETAPA 1: BUILDER
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# 1. Copiar wrapper y archivos de configuración de Gradle para aprovechar caché de capas
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./

# Permisos de ejecución para el wrapper en Linux
RUN chmod +x gradlew

# Descargar dependencias de forma aislada
RUN ./gradlew dependencies --no-daemon || true

# 2. Copiar código fuente y compilar el JAR ejecutable
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ------------------------------------------------------------------------------
# ETAPA 2: PRODUCTION (DISTROLESS)
# ------------------------------------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot AS production

WORKDIR /app

# Argumentos de construcción para telemetría OCI
ARG BUILD_DATE="unknown"
ARG VCS_REF="unknown"
ARG IMAGE_TAG="v1.0.0"

# Metadatos OCI estándar para trazabilidad en ACR y escaneos de seguridad
LABEL org.opencontainers.image.title="interview-backend" \
      org.opencontainers.image.description="Interview AI Backend - Spring Boot REST API en Azure Container Apps" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${IMAGE_TAG}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="https://github.com/vaninafuentes/Interview-backend_prats" \
      org.opencontainers.image.vendor="Universidad de Mendoza - Ingenieria de Software Aplicada" \
      author="vaninafuentes"

# Copiar el fat JAR generado desde la etapa builder con permisos de nonroot
COPY --from=builder --chown=nonroot:nonroot /workspace/build/libs/interview-back-0.0.1-SNAPSHOT.jar app.jar

# Optimización de JVM en contenedores:
#   - UseContainerSupport: Detecta límites de CPU y memoria de Azure Container Apps
#   - MaxRAMPercentage: Usa hasta el 75% de la RAM asignada (dejando el resto para el SO y metaspace)
#   - java.security.egd: Usa /dev/urandom para no bloquear arranque por entropía
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Usuario no privilegiado (UID 65532 de Distroless)
USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
