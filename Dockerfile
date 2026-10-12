# ==============================================================================
# Imagen del backend — construcción multietapa
# ==============================================================================
# Etapa 1 compila el JAR con un JDK y Gradle.
# Etapa 2 ejecuta solo el JAR, sobre una imagen distroless sin shell ni
# gestor de paquetes y con un usuario sin privilegios.
# Detalle en docs/docker.md
# ==============================================================================

# ------------------------------------------------------------------------------
# Etapa 1: compilación
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Los archivos de build se copian antes que el código para aprovechar la caché
# de capas: mientras no cambien las dependencias, no se vuelven a descargar.
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# Los tests se ejecutan en el pipeline, donde hay una base de datos disponible.
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ------------------------------------------------------------------------------
# Etapa 2: ejecución
# ------------------------------------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot AS production

WORKDIR /app

# Argumentos de trazabilidad que inyecta el pipeline en cada construcción.
ARG BUILD_DATE="unknown"
ARG VCS_REF="unknown"
ARG IMAGE_TAG="v1.0.0"

LABEL org.opencontainers.image.title="interview-backend" \
      org.opencontainers.image.description="Interview AI Backend - Spring Boot REST API desplegada en Azure Container Apps" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${IMAGE_TAG}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="https://github.com/vaninafuentes/Interview-backend_prats" \
      org.opencontainers.image.vendor="Universidad de Mendoza - Ingenieria de Software Aplicada" \
      author="vaninafuentes"

COPY --from=builder --chown=nonroot:nonroot \
     /workspace/build/libs/interview-back-0.0.1-SNAPSHOT.jar app.jar

# UseContainerSupport: la JVM respeta los límites de CPU y memoria del contenedor.
# MaxRAMPercentage: usa hasta el 75 % de la RAM asignada y deja el resto al sistema.
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
