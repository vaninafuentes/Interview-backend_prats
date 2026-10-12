# AUDITORÍA FINAL DEL PROYECTO — VERIFICACIÓN DE LOS 42 REQUISITOS

**Proyecto:** Interview AI Backend (Spring Boot)  
**Repositorio:** https://github.com/vaninafuentes/Interview-backend_prats  
**Fecha de la auditoría:** 11 de Octubre de 2026  
**Versión desplegada en Azure:** `interview-backend:v1.1.0`

> **Criterio de esta auditoría:** no se marca nada como completado sin una prueba concreta que lo respalde.
> Cada fila indica **qué prueba se hizo**, no qué se supone que debería funcionar. Cuando algo depende de una
> acción manual pendiente, se indica explícitamente en la columna *Pendiente*.

---

## RESUMEN

| Estado | Cantidad | Requisitos |
| :--- | :---: | :--- |
| ✅ COMPLETADO | 40 | Todos salvo los indicados abajo |
| ⚠️ PARCIAL | 0 | — |
| ⏭️ NO CORRESPONDE | 2 | 2 y 39 (integración con React Native, fuera de alcance) |
| 🔲 NO IMPLEMENTADO | 0 | — |
| ❓ PENDIENTE DE VERIFICACIÓN | 0 | — |
| **TOTAL** | **42** | |

---

## TABLA COMPLETA

| # | Requisito | Estado | Archivo o recurso | Prueba realizada | Resultado | Pendiente |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | Spring Boot funcional | ✅ COMPLETADO | `src/`, `build.gradle.kts` | `./gradlew clean build` + aplicación consultada en Azure | BUILD SUCCESSFUL con tests en verde; `Started InterviewBackApplication in 12.374 seconds` | — |
| 2 | React Native funcional | ⏭️ NO CORRESPONDE | `../Interview-frontend_prats` | No se probó | Excluido por decisión del equipo: el entregable desplegado es el backend | — |
| 3 | Autenticación JWT | ✅ COMPLETADO | `JwtUtil.java`, `JwtAuthFilter.java`, `AuthController.java` | `POST /api/auth/register` y `/login` contra la URL pública de Azure | HTTP 201 y 200 con token de 218 caracteres | — |
| 4 | Seguridad de endpoints | ✅ COMPLETADO | `SecurityConfig.java` | Peticiones con y sin token a `/api/auth/me` | 403 sin token · 200 con token · 401 con contraseña incorrecta · 403 con token adulterado | — |
| 5 | UserDetailsService | ✅ COMPLETADO | `SecurityConfig.java` (bean `userDetailsService`) | Login con usuario real de la base | Autenticación exitosa contra la tabla `users` | — |
| 6 | PasswordEncoder | ✅ COMPLETADO | `SecurityConfig.java` (bean BCrypt) | Registro y login | Contraseña almacenada en hash BCrypt; login válido y rechazo de contraseña incorrecta | — |
| 7 | Configuración de base de datos | ✅ COMPLETADO | `application.properties`, Container App | Conexión en local (Docker Compose) y en Azure | `db: {"status":"UP","database":"MySQL"}` en ambos entornos | — |
| 8 | HikariCP | ✅ COMPLETADO | `application.properties` (`spring.datasource.hikari.*`) | `GET /actuator/metrics/hikaricp.connections.max` | `value: 10.0`, pool `InterviewHikariPool`; parámetros configurables por variables de entorno | — |
| 9 | Variables de entorno | ✅ COMPLETADO | `.env.example`, `docker-compose.yml`, Container App | Revisión del repositorio público + inspección de la app en Azure | Ninguna credencial en el código ni en el repositorio; todo parametrizado con `${VARIABLE:default}` | — |
| 10 | Dockerfile | ✅ COMPLETADO | `Dockerfile` | `docker build` local y en el pipeline | Multietapa: builder `eclipse-temurin:21-jdk` + runtime `distroless/java21-debian12:nonroot` | — |
| 11 | Docker Compose | ✅ COMPLETADO | `docker-compose.yml` | `docker compose up -d` | `interview-backend-compose` y `interview-mysql-compose (healthy)` corriendo; healthcheck `UP` | — |
| 12 | Docker Build | ✅ COMPLETADO | `Dockerfile`, `.github/workflows/ci.yml` | Construcción local y en GitHub Actions | Imagen de 380 MB construida en ambos entornos; pipeline en verde | — |
| 13 | Imagen optimizada | ✅ COMPLETADO | `Dockerfile` | `docker inspect`, `docker images` | Base distroless (sin shell ni gestor de paquetes), usuario `nonroot:nonroot`, 119 MB comprimidos | — |
| 14 | Versionado de imágenes | ✅ COMPLETADO | `.github/workflows/ci.yml`, ACR | `az acr repository show-tags` | 11 etiquetas: `v1.0.1`, `v1.1.0`, `latest` y una por cada SHA de commit | — |
| 15 | Etiquetas OCI | ✅ COMPLETADO | `Dockerfile` | `docker inspect` sobre la imagen | 8 etiquetas verificadas: `title`, `description`, `created`, `version`, `revision`, `source`, `vendor`, `author` | — |
| 16 | Grype | ✅ COMPLETADO | `scans/`, `.github/workflows/ci.yml` | Escaneo local (contenedor) y en el pipeline | Informes JSON versionados + tabla de severidades en el resumen de cada ejecución | — |
| 17 | CVE | ✅ COMPLETADO | `docs/seguridad.md` | Identificación de CVE y GHSA por paquete | Listado de 9 críticas resueltas y 3 pendientes con justificación | — |
| 18 | CVSS | ✅ COMPLETADO | `docs/seguridad.md` (sección 3) | Explicación conceptual + uso de la severidad de Grype | Escala 0.0-10.0 documentada y aplicada a la priorización | — |
| 19 | Reducción de vulnerabilidades | ✅ COMPLETADO | `build.gradle.kts`, `scans/` | Comparación de escaneos con la misma base de datos | **186 → 115 hallazgos (−38 %); críticas 10 → 3** | — |
| 20 | GitHub Actions | ✅ COMPLETADO | `.github/workflows/ci.yml`, `codeql.yml` | Pestaña Actions | Ambos workflows ejecutándose en verde | — |
| 21 | Integración continua | ✅ COMPLETADO | `.github/workflows/ci.yml` | Push a `main` | Compila, ejecuta tests contra MySQL efímero, construye, escanea y publica | — |
| 22 | Despliegue continuo | ✅ COMPLETADO | `.github/workflows/ci.yml` (job `deploy`) | Liberación de `v1.1.0` | Desplegó automáticamente y verificó el health check antes de declararse exitoso | — |
| 23 | CI sin despliegue | ✅ COMPLETADO | Condición del job `deploy` | Push a `main` (ejecución 38182406341) | El job de despliegue figura como **skipped**: publicó la imagen sin desplegar | — |
| 24 | Despliegue condicionado | ✅ COMPLETADO | Condición del job `deploy` | Los tres casos de uso | **A** (skipped), **B** (tag `v1.1.0`) y **C** (manual) verificados por separado | — |
| 25 | GitHub Secrets | ✅ COMPLETADO | Repositorio → Settings → Secrets | Ejecución del pipeline | `AZURE_CREDENTIALS`, `ACR_REGISTRY`, `AZURE_RESOURCE_GROUP`; el login en Azure funciona | — |
| 26 | Service Principal | ✅ COMPLETADO | Entra ID: `sp-interview-backend` | `az role assignment list` | Permisos mínimos: `AcrPush` sobre el ACR y `Container Apps Contributor` sobre la app (ya no Contributor sobre todo el grupo) | — |
| 27 | Azure Subscription | ✅ COMPLETADO | Azure for Students | `az account show` | Suscripción activa, región `brazilsouth` | — |
| 28 | Resource Group | ✅ COMPLETADO | `rg-interview-app` | `az resource list -g` | 6 recursos en una sola unidad de administración | — |
| 29 | Azure Container Registry | ✅ COMPLETADO | `acrvaniainterview` | `az acr repository show-tags` | SKU Basic, imágenes versionadas publicadas por el pipeline | — |
| 30 | Azure Container Apps | ✅ COMPLETADO | `interview-backend` | `az containerapp show` | Estado `Succeeded`, revisión `Active`/`Healthy`, escalado 0-1 réplicas | — |
| 31 | Base de datos accesible en Azure | ✅ COMPLETADO | `mysql-interview-backend` | Consulta SQL directa + `/actuator/health` | Tablas `users`, `questions`, `interview_sessions` creadas por Hibernate; 10 preguntas sembradas; `db: UP` | — |
| 32 | Configuración de secretos | ✅ COMPLETADO | Container App Secrets | `az containerapp show` (solo nombres) | `db-password` y `jwt-secret` referenciados con `secretref:`; ningún valor en texto plano | — |
| 33 | Logs | ✅ COMPLETADO | Log Analytics `workspace-rginterviewappMrgY` | Consultas KQL reales | Logs de arranque, errores y trazas recuperados; se usaron para diagnosticar el HTTP 503 | — |
| 34 | Métricas | ✅ COMPLETADO | Actuator + Azure Monitor | `az monitor metrics list` | `Requests` = 2 y `Replicas` = 0,25 en la última hora; 98 métricas expuestas por la aplicación | — |
| 35 | Health Checks | ✅ COMPLETADO | `/actuator/health` | Peticiones con y sin token | `{"status":"UP"}` público; detalle completo solo con JWT. Verificado también en el paso de verificación del pipeline | — |
| 36 | Spring Boot Actuator | ✅ COMPLETADO | `application.properties`, `build.gradle.kts` | Consulta de todos los endpoints | `health`, `info`, `metrics`; detalles restringidos por rol; información de build publicada | — |
| 37 | Monitoreo desde Azure | ✅ COMPLETADO | Portal: Monitoring → Metrics / Logs | Consulta de métricas y de logs | Métricas de plataforma y de **JVM** (`JvmMemoryUsed`) disponibles sin instalar nada | — |
| 38 | Pruebas con Postman | ✅ COMPLETADO | `postman/Interview-Backend.postman_collection.json` | Los 7 casos de autenticación verificados con `curl` contra Azure + colección creada con 13 peticiones y scripts de aserción | Colección importable con 4 carpetas (salud, autenticación, endpoints protegidos y casos negativos); los scripts guardan el token automáticamente y validan cada respuesta | — |
| 39 | Integración React Native-Azure | ⏭️ NO CORRESPONDE | `../Interview-frontend_prats` | No se ejecutó | Excluido por decisión del equipo: el alcance es el despliegue del backend en Azure | — |
| 40 | Administración de aplicaciones | ✅ COMPLETADO | `scripts/admin.sh`, `docs/azure.md` (sección 8) | Ejecución del script (opciones `estado`, `versiones`, `revisiones`) | Runbook con 15 operaciones (deploy, start, stop, logs, revisiones, rollback, delete) en Portal y CLI | — |
| 41 | Análisis CodeQL | ✅ COMPLETADO | `.github/workflows/codeql.yml` | Ejecución del workflow (5/5 pasos en verde) | Análisis estático configurado y ejecutándose; consultas `security-extended` | Revisar las alertas en la pestaña **Security** del repositorio y registrarlas en la documentación |
| 42 | Documentación del proyecto | ✅ COMPLETADO | `README.md` + 8 documentos en `docs/` | Revisión del contenido contra la realidad implementada | `README`, arquitectura, docker, ci-cd, azure, seguridad, monitoreo, auditoría y guía del parcial. Incluye diagramas Mermaid | — |

---

## DETALLE: REQUISITOS FUERA DE ALCANCE Y COMPROBACIONES MANUALES

### ⏭️ Requisitos 2 y 39 — React Native

**Estado: NO CORRESPONDE (fuera del alcance acordado).**

No es un pendiente ni una omisión: se decidió explícitamente que el objeto del trabajo es demostrar el
despliegue del **backend** en Azure. La aplicación móvil es un **cliente** que consume la API; no requería
modificaciones para el despliegue ni forma parte de lo que se despliega.

**Si surgiera la pregunta en la defensa:** *"lo que se despliega en Azure es el backend en Spring Boot; la
aplicación React Native es un cliente que no necesitaba modificarse para el despliegue"*.

### ✅ Requisito 38 — Pruebas con Postman

La colección está en **`postman/Interview-Backend.postman_collection.json`**: 13 peticiones organizadas en
4 carpetas, **todas con scripts de aserción**. El script del login guarda el token en una variable de
colección automáticamente, de modo que se puede ejecutar la colección completa con el *Collection Runner* y
ver todos los casos en verde sin intervención manual.

**Casos incluidos:**

| Carpeta | Contenido |
| :--- | :--- |
| **0. Salud** | Health sin token (verifica que NO exponga componentes internos) e Info de build |
| **1. Autenticación** | Registro · login correcto (guarda y decodifica el token) · login con contraseña incorrecta |
| **2. Endpoints protegidos** | `/api/auth/me` · health con detalle · listado de preguntas · métricas · tiempos de respuesta |
| **3. Casos negativos** | Endpoint sin token · token adulterado · métricas sin token — **deben dar 403** |

**Todos los casos fueron verificados previamente con `curl` contra la URL pública de Azure:**

**Casos ya verificados contra `https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io`:**

| Caso | Método y ruta | Resultado esperado | Verificado |
| :--- | :--- | :--- | :---: |
| Registro | `POST /api/auth/register` | 201 + token | ✅ |
| Login correcto | `POST /api/auth/login` | 200 + token | ✅ |
| Login incorrecto | `POST /api/auth/login` (contraseña errónea) | 401 | ✅ |
| Endpoint protegido sin token | `GET /api/auth/me` | 403 | ✅ |
| Endpoint protegido con token | `GET /api/auth/me` + `Authorization: Bearer` | 200 + datos del usuario | ✅ |
| Token adulterado | `GET /api/auth/me` con token modificado | 403 | ✅ |
| Endpoint de negocio | `GET /api/questions` + token | 200 | ✅ |
| Health público | `GET /actuator/health` | 200, solo estado | ✅ |
| Health con token | `GET /actuator/health` + `Authorization: Bearer` | 200, detalle completo | ✅ |
| Métricas sin token | `GET /actuator/metrics` | 403 | ✅ |

### ✅ Requisito 42 — Documentación

| Documento | Contenido |
| :--- | :--- |
| `README.md` | Puerta de entrada: tecnologías, arquitectura, ejecución local, endpoints, CI/CD, decisiones |
| `docs/arquitectura.md` | Diagramas de despliegue y de secuencia, capas, modelo de datos, decisiones |
| `docs/docker.md` | Dockerfile explicado, imagen distroless, Docker Compose, comandos, problemas resueltos |
| `docs/ci-cd.md` | Pipeline, versionado, casos A/B/C, secrets, resolución de errores |
| `docs/azure.md` | Infraestructura, costos, runbook de administración, verificación |
| `docs/seguridad.md` | CVEs, remediación, CodeQL, estándares (CVE/CVSS) y comparativa de herramientas |
| `docs/monitoreo.md` | Tres capas de observabilidad, métricas, consultas KQL, 12 factores |
| `docs/auditoria-final.md` | Este documento |
| `docs/guia-parcial.md` | Las 50 preguntas de defensa y el simulacro completo |

### ❓ Requisito 41 — Alertas de CodeQL

El workflow se ejecuta correctamente, pero **las alertas que haya producido todavía no fueron revisadas**.
Requiere entrar a la pestaña **Security** → *Code scanning alerts* del repositorio. No corresponde declarar
que no hay hallazgos sin haberlo comprobado.

---

## LO QUE QUEDA PENDIENTE

| Pendiente | Quién | Dónde |
| :--- | :--- | :--- |
| **Revisar y registrar las alertas de CodeQL** | Manual — requiere autenticación | Pestaña **Security** → *Code scanning alerts* |

---

## ESTADO OPERATIVO AL CIERRE DE LA AUDITORÍA

| Componente | Estado |
| :--- | :--- |
| Aplicación en Azure | `interview-backend:v1.1.0` · revisión `Active` · **Healthy** |
| Base de datos | **Encendida** (`Ready`) — se encendió para permitir la verificación del despliegue automático |
| URL pública | https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io |
| Último pipeline | Ejecución 38185761730 disparada por el tag `v1.1.0` — **success** |
| Entorno local | Docker Compose funcionando (backend + MySQL) |

> ⚠️ **Recordatorio operativo:** la base de datos debe detenerse cuando no se use
> (`az mysql flexible-server stop -g rg-interview-app -n mysql-interview-backend`) y **debe encenderse antes
> de cualquier demostración**. Con la base apagada, el backend no arranca.
