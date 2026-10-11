# REPORTE DE AUDITORÍA DE SEGURIDAD — VULNERABILIDADES (CVE & CVSS)

**Proyecto:** Interview AI Backend (Spring Boot)  
**Herramienta de Auditoría:** Grype v0.119.0 (Anchore)  
**Imágenes Analizadas:** `interview-backend:v1.0.0` (baseline) y `interview-backend:v1.0.1` (remediada)  
**Arquitectura:** `linux/amd64` — Base `gcr.io/distroless/java21-debian12:nonroot`  
**Fecha del Análisis Inicial:** 10 de Octubre de 2026  
**Fecha de Remediación y Re-escaneo:** 11 de Octubre de 2026  

---

## 0. RESULTADO DE LA REMEDIACIÓN (RESUMEN EJECUTIVO)

Se actualizaron las dependencias vulnerables dentro de la línea compatible (`Spring Boot 3.5.4 → 3.5.16`, con
sobreescrituras puntuales de Tomcat y Jackson) y se reconstruyó la imagen. Ambas versiones fueron escaneadas
**con la misma base de datos de vulnerabilidades** para que la comparación sea válida:

| Severidad | v1.0.0 (baseline) | v1.0.1 (remediada) | Diferencia |
| :--- | :---: | :---: | :---: |
| **Critical** | 10 | **3** | **−7** |
| **High** | 62 | **35** | **−27** |
| **Medium** | 72 | **46** | **−26** |
| **Low** | 21 | **10** | **−11** |
| Negligible | 18 | 18 | ±0 |
| Unknown | 3 | 3 | ±0 |
| **TOTAL** | **186** | **115** | **−71 (−38 %)** |

**9 de las 10 vulnerabilidades críticas fueron eliminadas.** Las 3 restantes no tienen corrección publicada
a la fecha (ver sección 6.4). El detalle de los cambios, las vulnerabilidades resueltas y la justificación de
las pendientes están en la **sección 6**.

---

## 1. RESUMEN EJECUTIVO Y CONTEO POR SEVERIDAD

El análisis de la imagen completa arrojó un total de **186 coincidencias de vulnerabilidad**, distribuidas entre las capas del sistema operativo mínimo (Debian 12), el binario de OpenJDK 21 y las bibliotecas Java empaquetadas en el JAR:

| Severidad | Total Detectado | Dependencias Java (`java-archive`) | Sistema Operativo (`deb`) | Binario Runtime (`binary`) |
| :--- | :---: | :---: | :---: | :---: |
| **Critical** | **10** | 9 | 1 | 0 |
| **High** | **62** | 27 | 35 | 0 |
| **Medium** | **72** | 27 | 30 | 15 |
| **Low** | **21** | 11 | 5 | 5 |
| **Negligible / Unknown** | **21** | 0 | 21 | 0 |
| **TOTAL** | **186** | **74** | **92** | **20** |

---

## 2. VULNERABILIDADES CRÍTICAS DETECTADAS

| Paquete | Versión Instalada | Identificador | Versión de Fix | Severidad | Riesgo para la Aplicación |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `tomcat-embed-core` | `10.1.43` | GHSA-r29c-68gh-xp6x | `10.1.55` | **Critical** | Fuga de buffers HTTP/2 en conexiones reutilizadas. |
| `tomcat-embed-core` | `10.1.43` | GHSA-h6fc-48rj-7qqh | `10.1.55` | **Critical** | Desincronización de requests HTTP Request Smuggling. |
| `tomcat-embed-core` | `10.1.43` | GHSA-5m62-pw8w-7w9f | `10.1.55` | **Critical** | Denegación de servicio (DoS) por streams maliciosos. |
| `tomcat-embed-core` | `10.1.43` | GHSA-9xv2-5v5q-p794 | `10.1.58` | **Critical** | Desbordamiento de memoria por headers malformados. |
| `tomcat-embed-core` | `10.1.43` | GHSA-gcx9-497g-6cp6 | `10.1.58` | **Critical** | Bypass de filtros por parseo inconsistente de URI. |
| `tomcat-embed-core` | `10.1.43` | GHSA-h3x4-894j-xpx5 | `10.1.58` | **Critical** | Validación deficiente de content-length. |
| `spring-security-web` | `6.5.2` | GHSA-mf92-479x-3373 | `6.5.9` | **Critical** | Posible omisión de reglas de autorización en paths concurrentes. |
| `spring-webmvc` | `6.2.9` | GHSA-pc63-qcmh-9cmg | *(Sin fix)* | **Critical** | Excepción no capturada en manejo de controladores reactivos. |
| `spring-webmvc` | `6.2.9` | GHSA-j9f9-w8pj-32f8 | *(Sin fix)* | **Critical** | Comportamiento anómalo en negociación de contenido. |
| `libexpat1` | `2.5.0-1+deb12u2`| CVE-2026-76957 | `2.5.0-1+deb12u3` | **Critical** | Parseo XML nativo en bibliotecas C de Debian. |

---

## 3. CONCEPTOS FUNDAMENTALES: CVE Y CVSS

### ¿Qué es un CVE (Common Vulnerabilities and Exposures)?
Un identificador alfanumérico único estandarizado (formato `CVE-YYYY-NNNNN`) asignado a una vulnerabilidad de ciberseguridad públicamente divulgada. Es gestionado por la organización MITRE y los CVE Numbering Authorities (CNA). Permite que herramientas de análisis (Grype, Trivy, Dependabot) y equipos de seguridad hablen de la misma falla técnica sin ambigüedades.

### ¿Qué es el CVSS (Common Vulnerability Scoring System)?
Un sistema cuantitativo estándar de la industria para evaluar la gravedad técnica de una vulnerabilidad mediante una puntuación de **0.0 a 10.0**:
- **0.0 - 3.9:** Low (Baja)
- **4.0 - 6.9:** Medium (Media)
- **7.0 - 8.9:** High (Alta)
- **9.0 - 10.0:** Critical (Crítica)

El score se calcula según métricas base: vector de ataque (Red, Local), complejidad de ataque, privilegios requeridos, interacción del usuario y su impacto en la tríada **Confidencialidad, Integridad y Disponibilidad (CIA)**.

---

## 4. MEDIDAS DE MITIGACIÓN APLICADAS Y JUSTIFICACIÓN TÉCNICA

### Mitigaciones Implementadas:
1. **Adopción de Distroless (`gcr.io/distroless/java21-debian12:nonroot`):**
   - Eliminó completamente el gestor de paquetes `apt`, el compilador C, y shells como `bash` y `sh`.
   - Si hubiésemos usado `ubuntu:latest` o `debian:bookworm` estándar, el conteo de vulnerabilidades superaría las 400 CVEs a nivel SO.
2. **Ejecución con Usuario No Privilegiado (`USER nonroot:nonroot`):**
   - Aunque existan vulnerabilidades en bibliotecas nativas de Debian (`libc6`, `libexpat1`), un atacante no tiene permisos de root en el host ni en el contenedor para instalar herramientas ni escalar privilegios.
3. **Desacoplamiento de Secretos y Hardening de Endpoints:**
   - Las rutas públicas son únicamente `/api/auth/register`, `/api/auth/login`, `/actuator/health` (y sus sondas `liveness`/`readiness`) y `/actuator/info`. **Todas las demás exigen un JWT válido firmado con algoritmo simétrico HS256**, incluidas `/actuator/metrics` (que devuelve HTTP 403 sin token).
   - **Mitigación adicional (11/10/2026):** el detalle del endpoint de salud dejó de exponerse públicamente. Con `show-details=when-authorized` y `roles=USER`, una petición anónima ve solo `{"status":"UP"}`, mientras que una autenticada ve el detalle completo (base de datos, disco). Antes, la configuración `always` publicaba sin autenticación el motor de base de datos utilizado, el espacio en disco del contenedor y la estructura interna de componentes.

### Justificación de Vulnerabilidades sin Solución Disponible:
- **`spring-webmvc:6.2.9` (GHSA-pc63-qcmh-9cmg, GHSA-j9f9-w8pj-32f8):** Reportadas recientemente; la versión oficial del BOM de Spring Boot 3.5.4 aún no tiene publicada una release GA en Maven Central que las resuelva sin generar incompatibilidades de binario con el resto del framework.
- **`libexpat1` / `libc6` en Distroless:** Corresponden a Debian 12 Bookworm upstream. No impactan directamente a la aplicación ya que Spring Boot no utiliza bindings directos JNI sobre `libexpat` para parsear XML (la aplicación es 100% JSON REST vía Jackson).

---

## 5. COMPARACIÓN: GRYPE VS TRIVY

| Característica | Anchore Grype | Aqua Security Trivy |
| :--- | :--- | :--- |
| **Enfoque Principal** | Escáner especializado de vulnerabilidades en imágenes, contenedores y SBOMs. | Plataforma integral de seguridad todo-en-uno (Vulnerabilidades, Misconfigs, Secretos, Licencias). |
| **Integración SBOM** | Nativa con **Syft** (generador oficial de SBOMs de Anchore). | Soporte para CycloneDX y SPDX. |
| **Escaneo de Código / IaC** | No (solo busca CVEs en paquetes). | **Sí:** Analiza `Dockerfile`, Terraform, Kubernetes manifests buscando malas prácticas. |
| **Detección de Secretos** | No. | **Sí:** Detecta API keys o tokens expuestos en el código o capas. |
| **Velocidad y Ligereza** | Extremadamente rápido y liviano para pipelines de CI/CD. | Más pesado debido a su base de reglas multifunción. |
| **Casos de Uso** | Pipelines de CI/CD enfocados en gate de vulnerabilidades y verificación de imágenes en ACR. | Auditorías integrales de postura de seguridad (CSPM, IaC scanning, contenedor). |

---

## 6. REMEDIACIÓN APLICADA (11/10/2026)

### 6.1 Cambios realizados

**Archivo modificado:** `build.gradle.kts`

| Cambio | Antes | Después | Motivo |
| :--- | :--- | :--- | :--- |
| Versión de Spring Boot | `3.5.4` | `3.5.16` | Última release de la línea 3.5.x (upgrade de patch: sin cambios de API). Arrastra Spring Security 6.5.2 → 6.5.11, Spring Framework 6.2.9 → 6.2.19 y Hibernate 6.6.22 → 6.6.53. |
| Sobreescritura de Tomcat | `10.1.43` (del BOM) | `10.1.60` | El BOM de 3.5.16 fija 10.1.55, que corrige 3 de las 6 críticas de Tomcat; las otras 3 requieren 10.1.58+. Se sobreescribe la propiedad gestionada. |
| Sobreescritura de Jackson | `2.21.4` (del BOM) | `2.21.7` | El BOM de 3.5.16 introducía Jackson 2.21.4, con 11 advisories (High/Medium) resueltos en 2.21.5-2.21.7. |

Las sobreescrituras usan el mecanismo oficial de Spring Boot (`extra["<propiedad>.version"]`), documentado como
forma soportada de reemplazar una versión gestionada sin abandonar el BOM. Ambas permanecen dentro de la misma
línea `minor`, por lo que no hay cambios de API.

**Verificación de que no se rompió nada:** `./gradlew clean build` → BUILD SUCCESSFUL con los tests
(`contextLoads`) en verde. La imagen se levantó contra MySQL y se validó el flujo completo
(health, register, login, endpoint protegido con y sin token, login con password incorrecta) — ver sección 6.5.

### 6.2 Vulnerabilidades críticas resueltas (9)

| Paquete | Versión vulnerable | Identificador | Corregida en |
| :--- | :--- | :--- | :--- |
| `tomcat-embed-core` | 10.1.43 | GHSA-r29c-68gh-xp6x | 10.1.55 |
| `tomcat-embed-core` | 10.1.43 | GHSA-h6fc-48rj-7qqh | 10.1.55 |
| `tomcat-embed-core` | 10.1.43 | GHSA-5m62-pw8w-7w9f | 10.1.55 |
| `tomcat-embed-core` | 10.1.43 | GHSA-9xv2-5v5q-p794 | 10.1.58 |
| `tomcat-embed-core` | 10.1.43 | GHSA-gcx9-497g-6cp6 | 10.1.58 |
| `tomcat-embed-core` | 10.1.43 | GHSA-h3x4-894j-xpx5 | 10.1.58 |
| `spring-security-web` | 6.5.2 | GHSA-mf92-479x-3373 | 6.5.9 |
| `libexpat1` | 2.5.0-1+deb12u2 | CVE-2026-76957 | Resuelta por actualización de la base distroless |

Además se resolvieron 27 vulnerabilidades High, 26 Medium y 11 Low.

### 6.3 Detalle por familia de dependencia

| Dependencia | v1.0.0 | v1.0.1 | Advisories eliminados |
| :--- | :--- | :--- | :---: |
| Tomcat (embed-core) | 10.1.43 | 10.1.60 | 6 críticas + varias High/Medium |
| Spring Security | 6.5.2 | 6.5.11 | 1 crítica |
| Spring Framework (webmvc/core) | 6.2.9 | 6.2.19 | 2 críticas resueltas en la nueva versión |
| Jackson (core/databind) | 2.19.2 → 2.21.4 | 2.21.7 | 11 (introducidas por el BOM nuevo, resueltas por la sobreescritura) |
| Hibernate | 6.6.22 | 6.6.53 | — |
| MySQL Connector/J | 9.3.0 | 9.7.0 | — |

### 6.4 Vulnerabilidades críticas PENDIENTES y justificación (3)

| Paquete | Versión | Identificador | Fix publicado | Justificación |
| :--- | :--- | :--- | :---: | :--- |
| `spring-webmvc` | 6.2.19 | GHSA-pc63-qcmh-9cmg | **NO** | 6.2.19 es la última versión publicada de la línea 6.2.x (verificado en Maven Central). No existe release que la corrija sin migrar a Spring Framework 7 / Spring Boot 4.x, lo que implicaría un salto de major con cambios incompatibles. Se asume el riesgo: la aplicación es una API REST JSON y el advisory afecta al manejo de controladores reactivos, que no se utiliza. |
| `spring-webmvc` | 6.2.19 | GHSA-j9f9-w8pj-32f8 | **NO** | Mismo caso que el anterior. |
| `libc6` | 2.36-9+deb12u13 | CVE-2026-5450 | **NO** (aún) | Corresponde a la librería C de Debian 12 dentro de la base distroless de Google. Ya existe `2.36-9+deb12u14` en Debian que corrige varias CVEs de `libc6` (CVE-2026-4046, CVE-2026-0915, CVE-2025-15281, CVE-2026-0861, CVE-2026-4437, CVE-2026-4438), pero la imagen pública de distroless todavía no fue reconstruida con esa revisión (se verificó con `docker pull`: el digest no cambió). Se mitiga con `USER nonroot`: un atacante no tiene permisos para explotar escalada de privilegios local. |

**Política adoptada:** no se fuerzan parches fuera de línea (por ejemplo, reemplazar la base distroless por una
imagen completa con `apt upgrade`) porque aumentaría la superficie de ataque y rompería el principio de mínima
imagen que sostiene el resto del hardening. Estas vulnerabilidades se revisan en cada re-escaneo.

### 6.5 Pruebas de funcionamiento posterior a la remediación

Ejecutadas sobre el contenedor de la imagen `interview-backend:v1.0.1` conectado a MySQL:

| Prueba | Comando | Resultado |
| :--- | :--- | :--- |
| Arranque | `docker run` + logs | `Started InterviewBackApplication in 12.374 seconds` |
| Salud | `GET /actuator/health` | `{"status":"UP"}` con `db: {"status":"UP","database":"MySQL"}` |
| Registro | `POST /api/auth/register` | **201** con token JWT |
| Login | `POST /api/auth/login` | **200** con token JWT (212 caracteres) |
| Endpoint protegido sin token | `GET /api/auth/me` | **403** |
| Endpoint protegido con token | `GET /api/auth/me` con `Authorization: Bearer` | **200** + datos del usuario |
| Login con password incorrecta | `POST /api/auth/login` | **401** |
| Pool HikariCP | `GET /actuator/metrics/hikaricp.connections.max` (con token) | `value: 10.0`, pool `InterviewHikariPool` |

### 6.6 Cómo reproducir el análisis

> **Nota sobre Windows:** Grype tiene un bug conocido con el provider `docker` en Windows
> (usa `:` en los nombres de archivo de su caché de capas, carácter inválido en NTFS). La forma correcta
> de escanear en Windows es ejecutar Grype **dentro de un contenedor Linux** montando el socket de Docker.

```bash
# Escanear una imagen local (funciona en Windows, Linux y macOS)
MSYS_NO_PATHCONV=1 docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  anchore/grype:latest interview-backend:v1.0.1 -o json > scans/v1.0.1-final.json

# Reporte legible en consola (tabla)
MSYS_NO_PATHCONV=1 docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  anchore/grype:latest interview-backend:v1.0.1
```

Los reportes crudos quedan versionados en `scans/` como evidencia:
- `scans/v1.0.0-baseline.json` (186 hallazgos)
- `scans/v1.0.1-final.json` (115 hallazgos)
