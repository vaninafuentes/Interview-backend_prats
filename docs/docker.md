# DOCKER — CONSTRUCCIÓN, IMAGEN Y ENTORNO LOCAL

**Archivos:** `Dockerfile`, `.dockerignore`, `docker-compose.yml`  
**Imagen:** `acrvaniainterview.azurecr.io/interview-backend`  
**Tamaño:** 380 MB en disco (119 MB comprimida en el registro) · `linux/amd64`

---

## 1. DIFERENCIA ENTRE DOCKERFILE Y DOCKER COMPOSE

Es una pregunta habitual y la respuesta es corta: **resuelven problemas distintos y no son alternativas.**

| | **Dockerfile** | **Docker Compose** |
| :--- | :--- | :--- |
| **Qué define** | Cómo se **construye una imagen** | Cómo se **ejecutan uno o varios contenedores** |
| **Responde a** | "¿Qué contiene mi aplicación y cómo arranca?" | "¿Qué servicios levanto, cómo se conectan y con qué configuración?" |
| **Unidad** | Una imagen | Un conjunto de servicios, redes y volúmenes |
| **Se usa en** | `docker build` | `docker compose up` |
| **En este proyecto** | Construye la imagen del backend | Levanta el backend **y su base de datos MySQL** con red, volumen y healthcheck |

**En una frase:** el Dockerfile construye la imagen; Docker Compose la ejecuta junto al resto del entorno.

---

## 2. EL DOCKERFILE EXPLICADO

### Etapa 1 — Builder (compilación)

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Se copian PRIMERO el wrapper y los archivos de build, y recién después el código.
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./

RUN chmod +x gradlew

# Descarga las dependencias en una capa propia
RUN ./gradlew dependencies --no-daemon || true

# Ahora sí, el código fuente
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon
```

**Tres decisiones que vale la pena explicar:**

1. **El orden de los `COPY` no es casual.** Docker cachea cada instrucción en una capa. Si se copiara todo el
   código de una vez, cualquier cambio en un archivo `.java` invalidaría la descarga de dependencias y se
   volverían a bajar todas. Copiando primero los archivos de build y después el código, **mientras no cambien
   las dependencias, esa capa se reutiliza** y la construcción pasa de minutos a segundos.

2. **`chmod +x gradlew`** es necesario porque el bit de ejecución no siempre viaja bien desde Windows (ver
   *Problemas conocidos* al final).

3. **`-x test`** en el empaquetado: los tests **no se omiten del proyecto**, se ejecutan en la etapa de CI
   (con un MySQL real disponible). Volver a correrlos durante el empaquetado sería repetir el mismo trabajo
   y, además, obligaría a tener una base de datos durante el `docker build`. Los tests se ejecutan **una vez**,
   en el lugar correcto.

### Etapa 2 — Producción (runtime)

```dockerfile
FROM gcr.io/distroless/java21-debian12:nonroot AS production

WORKDIR /app

ARG BUILD_DATE="unknown"
ARG VCS_REF="unknown"
ARG IMAGE_TAG="v1.0.0"

LABEL org.opencontainers.image.title="interview-backend" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${IMAGE_TAG}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="https://github.com/vaninafuentes/Interview-backend_prats" \
      ...

COPY --from=builder --chown=nonroot:nonroot \
     /workspace/build/libs/interview-back-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Por qué una construcción multietapa:** la imagen final **no contiene** el JDK, ni Gradle, ni el código
fuente, ni las dependencias de compilación. Solo el JAR y una JRE. El resultado pasa de más de 1 GB a
**380 MB**, y —más importante— reduce drásticamente la superficie de ataque.

**Por qué distroless:** `gcr.io/distroless/java21-debian12` contiene **únicamente** la JRE y las bibliotecas
mínimas para ejecutarla. **No tiene shell** (`sh`, `bash`), **no tiene gestor de paquetes** (`apt`), ni
compilador de C, ni utilidades del sistema. Consecuencias prácticas:

- Un atacante que logre ejecutar código **no encuentra herramientas** con las que moverse lateralmente.
- El escaneo con Grype reporta **muchas menos vulnerabilidades**: no existen los paquetes del sistema
  operativo que no se usan. Con una imagen `ubuntu:latest` el conteo superaría las 400 CVEs de sistema.

**Optimización de la JVM en contenedores:**

| Opción | Para qué |
| :--- | :--- |
| `-XX:+UseContainerSupport` | La JVM lee los límites de CPU y memoria **del contenedor**, no del host |
| `-XX:MaxRAMPercentage=75.0` | Usa hasta el 75 % de la RAM asignada y deja el resto para el sistema operativo y el metaspace |
| `-Djava.security.egd=file:/dev/./urandom` | Evita que el arranque se bloquee esperando entropía |

**Usuario no privilegiado:** `USER nonroot:nonroot` (UID 65532). El proceso **no corre como root** dentro del
contenedor. Si alguien explotara una vulnerabilidad, no tendría privilegios para escalar ni instalar nada.

### Etiquetas OCI (trazabilidad)

| Etiqueta | Contenido | Origen |
| :--- | :--- | :--- |
| `org.opencontainers.image.created` | Fecha de construcción | `--build-arg BUILD_DATE` |
| `org.opencontainers.image.revision` | **SHA del commit** | `--build-arg VCS_REF` |
| `org.opencontainers.image.version` | Versión de la imagen | `--build-arg IMAGE_TAG` |
| `org.opencontainers.image.source` | Repositorio de origen | Fijo en el Dockerfile |

Los tres argumentos los inyecta **el pipeline** en cada construcción. Se verifican con:

```bash
docker inspect acrvaniainterview.azurecr.io/interview-backend:v1.1.0 \
  --format '{{json .Config.Labels}}'
```

---

## 3. `.dockerignore`

Excluye del **contexto de construcción** (lo que Docker envía al demonio) todo lo que no debe entrar:

| Excluido | Por qué |
| :--- | :--- |
| `.git/`, `.gitignore` | No hacen falta para compilar y pesan mucho |
| `build/`, `bin/`, `.gradle/` | Artefactos locales: dentro del contenedor se compila de cero |
| `.env`, `*.env` | **Credenciales locales que nunca deben entrar a una imagen** |
| `.idea/`, `.vscode/`, `*.iml` | Configuración del editor |
| `*.md`, `docs/` | Documentación: no aporta nada al runtime |
| `*.bat`, `*.cmd` | Scripts de Windows, inútiles en el contenedor Linux |

**Por qué importa:** sin `.dockerignore`, un `.env` con credenciales reales podría terminar **dentro de la
imagen** y quedar publicado en el registro. Es una de las fugas de secretos más comunes.

---

## 4. DOCKER COMPOSE

```yaml
services:
  db:                                   # MySQL 8 con persistencia
    image: mysql:8
    environment: [MYSQL_ROOT_PASSWORD, MYSQL_DATABASE]
    ports: ["${MYSQL_PORT:-3307}:3306"]
    volumes: [mysql_compose_data:/var/lib/mysql]
    networks: [interview-network]
    healthcheck:                        # mysqladmin ping cada 5s
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", ...]
      interval: 5s
      retries: 10
      start_period: 15s

  backend:                              # Spring Boot
    build: { context: ., dockerfile: Dockerfile }
    image: interview-backend:compose
    depends_on:
      db:
        condition: service_healthy      # NO arranca hasta que MySQL esté listo
    ports: ["${PORT:-8080}:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/...   # "db" = nombre del servicio
      ...
    networks: [interview-network]

networks:
  interview-network: { driver: bridge }

volumes:
  mysql_compose_data:
    name: mysql_compose_prats_data
```

**Cuatro decisiones explicadas:**

1. **`depends_on` con `condition: service_healthy`** (no solo `depends_on: [db]`). Sin la condición de
   salud, Compose arrancaría el backend apenas el contenedor de MySQL **existe**, no cuando está **listo**,
   y Spring Boot moriría intentando conectarse. Este es exactamente el error que causaba el HTTP 503 en
   Azure y que se diagnosticó con los logs.

2. **La URL usa `db` como host**, no `localhost`. Dentro de la red de Compose, cada servicio se resuelve por
   su nombre. `localhost` apuntaría al propio contenedor del backend, donde no hay base de datos.

3. **Volumen con nombre** (`mysql_compose_prats_data`): los datos **sobreviven** a `docker compose down`.
   Sin volumen, cada reinicio empezaría con la base vacía.

4. **Puerto 3307 en el host** (no 3306): evita el conflicto con un MySQL ya instalado en la máquina. El
   contenedor sigue usando 3306 internamente.

---

## 5. COMANDOS

### Construcción y ejecución

```bash
# Construir la imagen con los argumentos de trazabilidad
docker build \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg VCS_REF=$(git rev-parse HEAD) \
  --build-arg IMAGE_TAG=v1.1.0 \
  -t interview-backend:v1.1.0 .

# Ejecutar contra una base ya existente
docker run -d --name backend-test -p 8081:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3307/interviewai_db?..." \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e JWT_SECRET="clave-de-prueba-de-al-menos-32-caracteres" \
  interview-backend:v1.1.0

# Ver los logs
docker logs -f backend-test

# Inspeccionar la imagen
docker inspect interview-backend:v1.1.0 --format '{{.Config.User}}'   # nonroot:nonroot
docker history interview-backend:v1.1.0                                # capas
```

### Docker Compose

```bash
docker compose up -d              # levantar todo en segundo plano
docker compose up -d --build      # reconstruir la imagen antes de levantar
docker compose up -d --force-recreate backend   # forzar la recreación del contenedor
docker compose ps                 # ver el estado y los puertos
docker compose logs -f backend    # seguir los logs del backend
docker compose stop               # detener (conserva datos y contenedores)
docker compose down               # detener y eliminar contenedores (el volumen se conserva)
docker compose down -v            # ⚠️ elimina TAMBIÉN el volumen: se pierden los datos
```

> **Atención:** `docker compose up -d --build` no siempre recrea el contenedor si la etiqueta de la imagen
> no cambió. Si se reconstruyó el código y no se ven los cambios, usar `--force-recreate`.

### Publicación en el registro

```bash
az acr login --name acrvaniainterview
docker tag interview-backend:v1.1.0 acrvaniainterview.azurecr.io/interview-backend:v1.1.0
docker push acrvaniainterview.azurecr.io/interview-backend:v1.1.0
```

> En el flujo normal **esto no se hace a mano**: lo hace el pipeline de GitHub Actions.

---

## 6. PROBLEMAS CONOCIDOS Y CÓMO SE RESOLVIERON

### 6.1 `./gradlew: Permission denied` en Linux

**Síntoma:** el pipeline falla en el paso de compilación y en el `docker build` si no se hace `chmod`.

**Causa:** Windows no registra el bit de ejecución de los archivos (`core.fileMode=false`). Al comitear
`gradlew` desde Windows, quedó guardado con modo `100644` (no ejecutable) y en Linux no se puede ejecutar.

**Solución (dos lugares, por razones distintas):**

| Dónde | Qué se hizo | Por qué |
| :--- | :--- | :--- |
| En el repositorio | `git update-index --chmod=+x gradlew` → modo `100755` | Para que funcione en cualquier clon nuevo y en el CI |
| En el Dockerfile | `RUN chmod +x gradlew` | Porque el contexto de construcción puede venir de un sistema que no conserva el permiso |

Se agregó además `.gitattributes` con `gradlew text eol=lf` para fijar los finales de línea: un `gradlew`
con CRLF produce `bad interpreter: No such file or directory`.

### 6.2 La imagen no se actualiza al reconstruir con Compose

**Síntoma:** se reconstruye el código y el contenedor sigue con la versión anterior.
**Causa:** Compose no detecta el cambio si la etiqueta de la imagen es la misma.
**Solución:** `docker compose up -d --force-recreate backend`.

### 6.3 Grype falla en Windows

**Síntoma:** `failed to catalog: unable to place layer cache path`.
**Causa:** bug del proveedor `docker` de Grype en Windows: usa `:` en los nombres de su caché de capas, y
NTFS no lo permite.
**Solución:** ejecutar Grype **dentro de un contenedor Linux** montando el socket de Docker (documentado en
`docs/seguridad.md`, sección 6.6).

---

## 7. VERIFICACIÓN DE LA IMAGEN

```bash
# ¿Corre como root?  → NO (nonroot:nonroot)
docker inspect interview-backend:v1.1.0 --format '{{.Config.User}}'

# ¿Podría alguien abrir una shell dentro si entrara?  → NO existe shell
docker run --rm interview-backend:v1.1.0 sh
# Error: executable file not found in $PATH   ← comportamiento esperado en distroless

# ¿Qué contiene la imagen?  → solo el JAR y la JRE
docker run --rm --entrypoint java interview-backend:v1.1.0 -version

# ¿Hay secretos dentro de la imagen?  → NO
docker run --rm --entrypoint java interview-backend:v1.1.0 -jar app.jar --help >/dev/null
# Las credenciales se inyectan en tiempo de ejecución por variables de entorno
```

> **Nota:** el comando `docker run ... sh` **debe fallar**. Si abriera una shell, significaría que la imagen
> no es distroless y se perdió la principal ventaja de seguridad.
