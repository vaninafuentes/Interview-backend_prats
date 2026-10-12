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

---

## 7. CODEQL — ANÁLISIS ESTÁTICO DEL CÓDIGO (SAST)

**Archivo:** `.github/workflows/codeql.yml`  
**Primera ejecución verificada:** 11 de Octubre de 2026 — los 5 pasos en verde

### 7.1 ¿Qué es CodeQL?

Es el motor de análisis estático de GitHub. Convierte el código fuente en una **base de datos consultable**
y ejecuta sobre ella un conjunto de consultas que buscan **patrones de código inseguro**. Es la técnica
conocida como **SAST** (*Static Application Security Testing*): analiza el código **sin ejecutarlo**.

### 7.2 Diferencias con Grype

| | **Grype** | **CodeQL** |
| :--- | :--- | :--- |
| **Qué analiza** | Dependencias e imagen Docker | **Código fuente propio** |
| **Cómo lo hace** | Compara versiones contra bases de datos de vulnerabilidades (CVE) | Consultas sobre una base de datos generada a partir del código |
| **Qué encuentra** | "Tenés Tomcat 10.1.43, que tiene una CVE conocida" | "Este método nuestro concatena una consulta SQL con entrada del usuario" |
| **Cuándo detecta** | Cuando la vulnerabilidad ya fue publicada por alguien | Vulnerabilidades **propias, aún no explotadas** |
| **Ejecuta la aplicación** | No | No |

**Son complementarios, no alternativos.** Grype cubre el riesgo *heredado* (bibliotecas de terceros);
CodeQL cubre el riesgo *propio* (código que escribimos nosotros).

> ⚠️ **Punto importante para la defensa:** CodeQL analiza **estáticamente**. **No bloquea ataques SQL en
> tiempo de ejecución** — solo advierte que ese patrón existe en el código. Quien evita la inyección SQL en
> ejecución es Hibernate/JPA con **consultas parametrizadas**, que es lo que usa este proyecto.

### 7.3 Qué busca

Inyección SQL, XSS, deserialización insegura, path traversal, criptografía débil, exposición de información
sensible, credenciales embebidas en el código, y otras familias de defectos de seguridad.

### 7.4 Cómo está configurado

```yaml
on:
  push:        { branches: [main] }
  pull_request:{ branches: [main] }
  schedule:    [ '37 6 * * 1' ]   # todos los lunes
  workflow_dispatch:
```

- **`queries: security-extended`** → conjunto ampliado de consultas de seguridad (más exhaustivo que el
  conjunto por defecto).
- **Compilación controlada:** se ejecuta `./gradlew compileJava` (solo código de producción, **sin tests**),
  porque el análisis estático no necesita una base de datos y ejecutar los tests exigiría levantar MySQL.
- **`permissions: security-events: write`** → necesario para publicar los resultados en la pestaña
  *Security* del repositorio.
- Se ejecuta también **una vez por semana** aunque el código no cambie: las consultas de CodeQL se actualizan
  con el tiempo y pueden aparecer hallazgos nuevos sobre código viejo.

En repositorios **públicos** el análisis de código es **gratuito**.

### 7.5 Cómo ver los resultados

1. Ir a https://github.com/vaninafuentes/Interview-backend_prats
2. Pestaña **Security** (arriba, entre *Wiki* e *Insights*)
3. En la barra lateral: **Code scanning alerts**
4. Cada alerta muestra: regla, severidad, archivo, línea y una **explicación del flujo de datos** que
   describe cómo la entrada del usuario llega hasta el punto vulnerable.

### 7.6 Las tres técnicas de análisis de seguridad del proyecto

| Técnica | Herramienta | Qué protege | Dónde se ejecuta |
| :--- | :--- | :--- | :--- |
| **Análisis de dependencias** (CVE/CVSS) | **Grype** | Bibliotecas de terceros e imagen base | `ci.yml`, en cada push |
| **Análisis estático de código** (SAST) | **CodeQL** | Código propio | `codeql.yml`, en cada push y semanalmente |
| **Análisis de la imagen** | **Grype** (sobre la imagen construida) | El contenedor completo tal como se despliega | `ci.yml`, antes de publicar |

---

## 8. ESTÁNDARES Y HERRAMIENTAS DE SEGURIDAD

> **Aclaración importante:** CVE y CVSS **no son herramientas**, son **estándares**. Es un error frecuente
> mezclarlos con los escáneres. Los escáneres *detectan*; CVE *nombra* lo detectado y CVSS *mide* su gravedad.

### 8.1 Los estándares (conceptos, no programas)

| Estándar | Qué es | Quién lo administra | Para qué sirve |
| :--- | :--- | :--- | :--- |
| **CVE**<br/>*Common Vulnerabilities and Exposures* | Un **identificador único** con formato `CVE-AAAA-NNNNN` asignado a cada vulnerabilidad divulgada públicamente | MITRE y las CNA | Que todos hablen de **la misma falla** con el mismo nombre. Sin esto, un reporte de Grype, uno de Trivy y uno de Snyk serían incomparables |
| **CVSS**<br/>*Common Vulnerability Scoring System* | La **puntuación de 0.0 a 10.0** que mide la gravedad técnica, calculada con métricas (vector de ataque, complejidad, privilegios, impacto en confidencialidad/integridad/disponibilidad) | FIRST | **Priorizar**: no se puede corregir todo a la vez, entonces se empieza por lo crítico |
| **GHSA**<br/>*GitHub Security Advisory* | Identificador de avisos de seguridad de GitHub | GitHub | Es el formato que usa Grype para las dependencias Java (los `GHSA-...` del informe) |
| **SBOM**<br/>*Software Bill of Materials* | Inventario de todos los componentes de un software | — | Saber **qué hay adentro** para poder responder rápido ante una vulnerabilidad nueva ("¿usamos Log4j?") |

**La analogía útil:** un CVE es el **número de expediente** de un delito; el CVSS es la **gravedad del delito**;
y Grype/Trivy/Snyk son los **peritos** que investigan y te traen el expediente.

### 8.2 Las herramientas de análisis (programas)

| Herramienta | Tipo | Qué analiza | Costo | ¿La usamos? |
| :--- | :--- | :--- | :--- | :---: |
| **Grype** (Anchore) | SCA + escaneo de imágenes | Dependencias e imagen Docker contra bases de CVE | Open source | ✅ **Sí** |
| **Syft** (Anchore) | Generador de SBOM | Inventario de componentes (alimenta a Grype) | Open source | Indirecto |
| **Trivy** (Aqua) | SCA + IaC + secretos | Imágenes, **Dockerfile**, Terraform, Kubernetes y **secretos filtrados** | Open source | ❌ No (evaluada) |
| **Snyk** | Plataforma comercial | Dependencias (SCA), código (SAST), contenedores, IaC | Plan gratuito limitado, luego pago | ❌ No (evaluada) |
| **Dependabot** | Bot de GitHub | Abre Pull Requests automáticos al detectar dependencias desactualizadas o vulnerables | Gratis en GitHub | ❌ No (evaluado) |
| **CodeQL** | SAST | **Código fuente propio** | Gratis en repos públicos | ✅ **Sí** |
| **OWASP Dependency-Check** | SCA | Dependencias contra la base del NVD | Open source | ❌ No |

### 8.3 Cómo se relacionan

```
                    ┌─────────────────────────────────────┐
   APLICACIÓN ────► │  SBOM (Syft)                        │
                    │  inventario de componentes          │
                    └──────────────┬──────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────────────┐
                    │  ESCÁNERES (Grype / Trivy / Snyk)   │
                    │  comparan contra bases de datos      │
                    └──────────────┬──────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────────────┐
                    │  ESTÁNDARES                          │
                    │  CVE → cómo se llama                 │
                    │  CVSS → cuán grave es                │
                    └─────────────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────────────┐
                    │  DECISIÓN: actualizar / mitigar /    │
                    │  aceptar el riesgo (documentado)     │
                    └─────────────────────────────────────┘
```

### 8.4 Por qué elegimos Grype + CodeQL y no Snyk ni Dependabot

| Alternativa | Por qué no |
| :--- | :--- |
| **Snyk** | Es una plataforma comercial: exige cuenta, token en el pipeline y tiene límites de escaneo en el plan gratuito. **Grype cubre la misma necesidad de análisis de dependencias sin cuenta ni límites**, y además escanea la imagen completa (no solo las dependencias). |
| **Dependabot** | Resuelve otro problema: **mantener actualizadas** las dependencias abriendo Pull Requests. Es complementario, no un reemplazo del escaneo. No se incorporó porque en este trabajo las actualizaciones se hicieron de forma **deliberada y verificada** (subir Spring Boot y re-escanear con Grype), y automatizarlas habría agregado ruido sin aportar a los objetivos del trabajo. Queda como mejora futura. |
| **Trivy** | Se evaluó: aporta análisis de IaC y detección de secretos, que Grype no tiene. No se incorporó para **no duplicar herramientas** con funciones superpuestas; esas dos capacidades quedaron cubiertas de otra forma (revisión manual del Dockerfile y `.dockerignore` + secretos gestionados en Container Apps). |
| **OWASP Dependency-Check** | Más lento y con más falsos positivos que Grype; requiere descargar la base del NVD, que demora mucho en el pipeline. |

**Criterio aplicado:** una herramienta por necesidad, sin solapamientos y sin dependencias externas
innecesarias. Las tres necesidades están cubiertas:

| Necesidad | Cubierta por |
| :--- | :--- |
| Vulnerabilidades en dependencias y en la imagen | **Grype** |
| Patrones inseguros en el código propio | **CodeQL** |
| Nomenclatura y priorización | **CVE** y **CVSS** |

### 8.5 Cómo se lee un informe de Grype

```bash
grype interview-backend:v1.1.0 -o table
```

```
NAME                 INSTALLED   FIXED-IN   TYPE          VULNERABILITY        SEVERITY
tomcat-embed-core    10.1.43     10.1.55    java-archive  GHSA-r29c-68gh-xp6x  Critical
spring-webmvc        6.2.19      -          java-archive  GHSA-pc63-qcmh-9cmg  Critical
libc6                2.36-...    -          deb           CVE-2026-5450        Critical
```

| Columna | Significado |
| :--- | :--- |
| `NAME` / `INSTALLED` | Qué paquete y qué versión tenés |
| `FIXED-IN` | **La versión que corrige el problema.** Si dice `-` (o "sin fix"), no existe corrección publicada todavía |
| `VULNERABILITY` | El identificador: `CVE-...` (NVD) o `GHSA-...` (GitHub) |
| `SEVERITY` | La gravedad según CVSS |

**La columna `FIXED-IN` es la que manda la decisión:** si hay versión de corrección, se actualiza; si no la
hay, se documenta y se mitiga.

---

## 9. RESULTADOS DE CODEQL — HALLAZGOS Y TRATAMIENTO

**Primer análisis:** 11/10/2026 · `security-extended` · **4 alertas** (1 alta, 3 medias)

> Este es el valor real del análisis estático: **Grype no habría encontrado ninguna de estas cuatro**, porque
> no son vulnerabilidades de bibliotecas sino patrones en el código que escribimos nosotros.

### 9.1 Alerta 1 — "Protección CSRF de Spring desactivada" (severidad ALTA)

**Ubicación:** `SecurityConfig.java:84` → `.csrf(AbstractHttpConfigurer::disable)`

**Veredicto: NO es una vulnerabilidad en este proyecto. Se descarta con justificación.**

**Por qué CodeQL la marca:** ve que se desactiva una protección de seguridad estándar y lo señala. El patrón
es real: la línea efectivamente desactiva CSRF.

**Por qué no aplica acá:** un ataque CSRF funciona porque **el navegador envía automáticamente las
credenciales** (cookies de sesión) en peticiones originadas desde otro sitio. La víctima visita una página
maliciosa y su navegador hace la petición al banco con la cookie puesta, sin que ella lo sepa.

Nuestra API no tiene ese vector:

| Condición para que exista CSRF | En este proyecto |
| :--- | :--- |
| El navegador envía credenciales **automáticamente** | ❌ No: el token viaja en la cabecera `Authorization: Bearer`, que **ningún navegador agrega solo**. Un sitio malicioso no puede forzarla |
| Hay **sesión con cookie** | ❌ No: `SessionCreationPolicy.STATELESS`, sin cookies de sesión |
| El cliente es un **formulario web** | ❌ No: es una API consumida por una app móvil y por Postman |

Desactivar CSRF en una API **sin estado, autenticada por token en cabecera** es la práctica **recomendada
por la propia documentación de Spring Security**.

**Lección sobre los analizadores estáticos:** CodeQL analiza el código, no la arquitectura completa. Ve la
línea `csrf().disable()` sin saber que no hay cookies ni sesiones. Esto **no invalida la herramienta** —
encontró 3 problemas reales que corregimos — pero muestra que **sus hallazgos requieren interpretación
humana**, no aceptación automática. Ese criterio es parte del trabajo de seguridad.

**Acción:** descartar la alerta en GitHub con el motivo *"Won't fix"* y un comentario explicando el análisis.

---

### 9.2 Alertas 2, 3 y 4 — "Inyección de registros" (severidad MEDIA)

**Ubicación:** `GroqService.java:90`, `:231` y `:380`
**Veredicto: SÍ son reales. Corregidas.**

**Qué es (CWE-117, *Improper Output Neutralization for Logs*):** si un valor controlado por el usuario
contiene saltos de línea y se escribe en el log sin neutralizar, se pueden **falsificar entradas del
registro**.

**El ataque, concreto:** el campo `topic` llega desde la petición HTTP. Si un atacante envía:

```
java\n2026-01-01 ERROR Falla crítica en la base de datos
```

en el archivo de logs aparecerían **dos líneas**: una real y otra que parece escrita por el sistema. Las
consecuencias son: dificultar el análisis forense, engañar a las alertas automáticas que buscan la palabra
`ERROR` y contaminar la evidencia. **No permite ejecutar código ni acceder a datos**: por eso la severidad
es media y no crítica.

**Corrección aplicada:** se agregó un método que neutraliza los caracteres de control antes de registrar:

```java
private static String sanitizarParaLog(String valor) {
    if (valor == null) {
        return "null";
    }
    // \p{Cntrl} coincide con todos los caracteres de control (incluye \n, \r, \t)
    return valor.replaceAll("\\p{Cntrl}", "_");
}
```

Y se aplicó a **6 sentencias de log**: las 3 marcadas por CodeQL (`topic`, `difficulty`, `category` —datos del
usuario— y el cuerpo de la respuesta de la API de IA) más otras 3 que registran cuerpos de respuesta HTTP
externa (`GroqService.java:147`, `:204` y `:320`), que presentan el mismo riesgo.

**Verificación del arreglo** (ejecutada, no supuesta):

| Entrada | Salida registrada | ¿Queda algún carácter de control? |
| :--- | :--- | :---: |
| `java` | `java` | no |
| `java\n2026-01-01 ERROR Falla grave` | `java_2026-01-01 ERROR Falla grave` | **no** |
| `tema\r\nERROR inyectado` | `tema__ERROR inyectado` | **no** |
| `tema\tcon\ttabulaciones` | `tema_con_tabulaciones` | **no** |
| `tema\0con byte nulo` | `tema_con byte nulo` | **no** |
| `null` | `null` | no |

El salto de línea se convierte en `_`, así que **la línea falsificada ya no puede existir**.

### 9.3 Cómo se cierran las alertas

| Alerta | Acción | Efecto |
| :--- | :--- | :--- |
| CSRF desactivado | **Descartar** en GitHub (*Won't fix* + justificación) | Queda registrada la decisión y el motivo |
| Inyección de registros ×3 | **Corregir el código** (hecho) | Se **cierran solas** en el siguiente análisis |

**Portal:** pestaña **Security** → *Code scanning alerts* → clic en la alerta → botón **Dismiss alert** →
elegir el motivo → escribir el comentario → confirmar.

### 9.4 Resumen del tratamiento

| Análisis | Hallazgos | Reales | Falsos positivos | Corregidos |
| :--- | :---: | :---: | :---: | :---: |
| **Grype** (dependencias e imagen) | 186 → 115 | 73 resueltas | — | ✅ |
| **CodeQL** (código propio) | 4 | 3 | 1 (CSRF, justificado) | ✅ |
