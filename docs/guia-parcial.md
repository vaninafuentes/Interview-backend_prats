# GUÍA DE DEFENSA — PREGUNTAS Y SIMULACRO

**Proyecto:** Interview AI Backend (Spring Boot en Microsoft Azure)  
**Repositorio:** https://github.com/vaninafuentes/Interview-backend_prats  
**URL del backend:** https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io

> **Cómo usar esta guía:** cada pregunta tiene **la respuesta técnica** (para entender), **qué decir en voz
> alta** (versión corta para el oral) y **cómo demostrarlo** (archivo y comando). Las demostraciones están
> probadas: no hay nada acá que no se haya ejecutado.

---

## 🔴 ANTES DE EMPEZAR LA DEMO

```bash
# 1. Encender la base (tarda ~2 minutos). Sin esto, el backend NO ARRANCA.
az mysql flexible-server start -g rg-interview-app -n mysql-interview-backend

# 2. Esperar a que diga "Ready"
az mysql flexible-server list -o table

# 3. Verificar que todo responda
bash scripts/admin.sh estado
```

**Recordar:** con la base apagada, el backend devuelve **HTTP 503**. Ese error ya lo vivimos al principio y
está diagnosticado en `docs/azure.md`.

---

# PARTE 1 — CI/CD Y GITHUB ACTIONS (preguntas 1 a 15)

### 1. ¿Qué es integración continua?

**Técnica:** Es la práctica de integrar los cambios al tronco principal con frecuencia (varias veces al día)
y **verificar cada integración automáticamente**: compilar, ejecutar tests y analizar el resultado. El
objetivo es detectar errores temprano, cuando son baratos de arreglar.

**En voz alta:** *"Es el robot que revisa cada cambio apenas lo subís: compila, corre los tests y si algo
falla, te avisa antes de que llegue a producción."*

**Demostración:** `.github/workflows/ci.yml` → pestaña **Actions** → cualquier ejecución.
Los tests corren contra un MySQL efímero que GitHub levanta solo para esa ejecución.

---

### 2. ¿Qué es despliegue continuo?

**Técnica:** Extender la automatización para llevar a producción cada versión que pasa las verificaciones,
sin intervención manual. En este proyecto está **desacoplado a propósito**: el CI (job 1) siempre construye
y publica, pero el CD (job 2) solo se ejecuta bajo una **condición explícita de liberación**.

**En voz alta:** *"Es que el despliegue también sea automático. En nuestro caso lo dejamos condicionado a
propósito: no queremos que cualquier cambio llegue solo a producción."*

---

### 3. ¿Qué sucede cuando hacemos un push a GitHub?

**Técnica:** Se dispara el workflow `ci.yml`:

```
Checkout → Java 21 → Compilar + TESTS (MySQL efímero) → Docker build
   → Grype (escaneo de vulnerabilidades) → Login en Azure (Service Principal)
   → Login en ACR → docker push  → ⏹️ EL DESPLIEGUE SE OMITE
```

Si los tests fallan, **el pipeline se detiene y no se publica nada**.

**En voz alta:** *"Se dispara el pipeline: compila, testea, construye la imagen, la escanea y la publica en
el registro. El despliegue no ocurre: queda omitido."*

**Demostración:** Actions → ejecución 38182406341 → el job *"Desplegar en Azure Container Apps"* figura como
**skipped**.

---

### 4. ¿Cómo podemos generar una imagen sin realizar el despliegue?

**Técnica:** La condición del job de despliegue:

```yaml
if: >-
  needs.build-scan-push.result == 'success' &&
  (startsWith(github.ref, 'refs/tags/v') || inputs.deploy == true)
```

Un **push normal a `main`** no cumple ninguna de las dos condiciones → el job queda `skipped`.

**En voz alta:** *"Haciendo un push a main. El pipeline construye y publica la imagen, pero el job de
despliegue ni siquiera arranca."*

---

### 5. ¿Cómo condicionamos el despliegue según rama, versión o commit?

**Técnica:** Con tres mecanismos en la misma expresión:

| Condición | Mecanismo | Cuándo |
| :--- | :--- | :--- |
| **Versión** | `startsWith(github.ref, 'refs/tags/v')` | Al pushear un tag `vX.Y.Z` |
| **Decisión manual** | `inputs.deploy == true` | Al ejecutar desde la interfaz |
| **Rama** | `on.push.branches: [main]` | El workflow solo corre en main |

**En voz alta:** *"Por etiqueta de versión, o marcando la casilla al ejecutarlo manualmente. Un push común a
main nunca despliega."*

---

### 6. ¿Dónde queda almacenada la imagen Docker versionada?

**Técnica:** En **Azure Container Registry**, `acrvaniainterview.azurecr.io`, repositorio
`interview-backend`, con una etiqueta por versión.

**Demostración:**
```bash
az acr repository show-tags --name acrvaniainterview --repository interview-backend -o tsv
# → v1.0.1, v1.1.0, latest, y una etiqueta por cada SHA de commit
bash scripts/admin.sh versiones
```

---

### 7. ¿Qué es Azure Container Registry?

**Técnica:** El servicio de registro privado de imágenes Docker de Azure, equivalente a Docker Hub pero
privado y dentro de la suscripción. Almacena las imágenes y las sirve a Container Apps. SKU **Basic**
(~USD 5/mes).

**En voz alta:** *"Es nuestro Docker Hub privado dentro de Azure. Ahí viven las versiones de la imagen."*

---

### 8. ¿De qué grupo de recursos depende nuestro ACR?

**Técnica:** De `rg-interview-app`, en la región `brazilsouth`.

**Demostración:** Portal → *Grupos de recursos* → `rg-interview-app` (se ven los 6 recursos juntos) o:
```bash
az resource list -g rg-interview-app -o table
```

---

### 9. ¿De qué suscripción depende el grupo de recursos?

**Técnica:** De la suscripción **Azure for Students** del tenant de la Universidad.

**Demostración:** `az account show`

```
Subscription → Resource Group → ACR → Imagen
                            └→ Container App → Backend
```

---

### 10. ¿Para qué utilizamos el Service Principal?

**Técnica:** Es una **identidad no humana** que GitHub Actions usa para autenticarse contra Azure. Permite
que el pipeline publique imágenes y despliegue **sin usar la cuenta personal de nadie**, y con permisos
acotados: `AcrPush` sobre el ACR y `Container Apps Contributor` sobre la aplicación.

**En voz alta:** *"Es como un usuario robot con permisos mínimos, para que el pipeline se autentique sin
depender de mi cuenta y sin poderse pasar de la raya."*

---

### 11. ¿Dónde utilizamos las credenciales del Service Principal?

**Técnica:** En el **GitHub Secret `AZURE_CREDENTIALS`**, que contiene el JSON con `clientId`,
`clientSecret`, `tenantId` y `subscriptionId`. El workflow lo lee con `${{ secrets.AZURE_CREDENTIALS }}` en
el paso `azure/login`.

**Dato clave:** las credenciales del Service Principal son **solo para GitHub Actions**. Spring Boot usa
**otras distintas** (usuario y contraseña de MySQL, secreto JWT) que viven en los secretos del Container
App. Nunca se cruzan.

---

### 12. ¿Cómo funciona la autenticación de GitHub Actions contra Azure?

**Técnica:** La action oficial `azure/login@v3` recibe el JSON del Service Principal, ejecuta el equivalente
a un `az login` con esas credenciales y **deja la sesión activa para los pasos siguientes**. Por eso después
alcanza con `az acr login` reutilizando la misma sesión, sin usuario administrador del registro.

**En voz alta:** *"El paso de login deja la sesión abierta, y los pasos siguientes ya operan como ese
Service Principal."*

---

### 13. ¿Cómo publicamos una imagen versionada?

**Técnica:** Hay dos caminos, y **los dos terminan en el pipeline**:

```bash
# Automático: cualquier push a main publica con etiqueta del SHA
git push origin main                      # → :6aec073 y :latest

# Liberación de versión: además publica la etiqueta semántica
git tag -a v1.1.0 -m "..." && git push origin v1.1.0   # → :v1.1.0 también
```

Internamente: `docker build` con varias `-t` (una sola imagen, varios nombres) y `docker push --all-tags`.

**Regla:** el despliegue **siempre** usa una etiqueta explícita, nunca `latest`.

---

### 14. ¿Cómo comprobamos que la imagen existe en el ACR?

```bash
az acr repository show-tags --name acrvaniainterview --repository interview-backend -o tsv
az acr manifest list-metadata --registry acrvaniainterview --name interview-backend
```

**Además, el propio pipeline lo verifica** antes de desplegar: si la etiqueta no existe, falla con un error
explícito y no deja la aplicación apuntando a una imagen inexistente.

---

### 15. ¿Cómo sabemos qué versión está ejecutándose en Azure?

```bash
az containerapp show -n interview-backend -g rg-interview-app \
  --query "properties.template.containers[0].image" -o tsv
# → acrvaniainterview.azurecr.io/interview-backend:v1.1.0
```

**Portal:** Container Apps → `interview-backend` → **Application → Revisions**.

**Por qué esto es posible:** cada imagen lleva la etiqueta del commit (`VCS_REF`) y la versión (`IMAGE_TAG`)
grabadas como **etiquetas OCI**, así que se puede rastrear del contenedor corriendo hasta el commit exacto.

---

# PARTE 2 — ADMINISTRACIÓN Y PRUEBAS (preguntas 16 a 22)

### 16. ¿Cómo detenemos una aplicación?

```bash
az containerapp stop -n interview-backend -g rg-interview-app
```

**Portal:** Container Apps → `interview-backend` → botón **Stop**.

> ⚠️ **Advertencia importante:** detener el Container App puede dejar el **entorno** marcado para
> auto-borrado (`ScheduledForDelete`), lo que rompe el próximo despliegue. **No se recomienda.** Como la
> aplicación está en `minReplicas: 0`, ya se apaga sola cuando no hay tráfico.

---

### 17. ¿Cómo iniciamos una aplicación?

```bash
az containerapp start -n interview-backend -g rg-interview-app
```

Si está escalada a cero, **no hace falta hacer nada**: la primera petición la despierta automáticamente
(tarda unos segundos en arrancar, es el *cold start*).

---

### 18. ¿Cómo eliminamos una aplicación?

```bash
az containerapp delete -n interview-backend -g rg-interview-app --yes
```

**Portal:** Container Apps → `interview-backend` → **Delete**.

**Consecuencias:** se pierden la configuración y los secretos de la aplicación. **No** se pierden la imagen
del ACR, la base de datos ni los logs. Es irreversible. Para eliminar **todo** al terminar el cuatrimestre:
`az group delete --name rg-interview-app --yes`.

**En el script:** `bash scripts/admin.sh` → opción 9, que exige escribir `ELIMINAR` para confirmar.

---

### 19. ¿Cómo consultamos los logs?

```bash
# En vivo (necesita al menos una réplica activa)
az containerapp logs show -n interview-backend -g rg-interview-app --tail 50

# Históricos (funciona siempre), vía Log Analytics
bash scripts/admin.sh  # opción 7
```

**Portal:** Container Apps → `interview-backend` → **Monitoring → Log stream** (en vivo) o **Logs**
(histórico, con KQL).

**Caso real:** con estos logs se diagnosticó el HTTP 503 — aparecía la cadena
`Connection refused → Communications link failure → JPA EntityManagerFactory → Unable to start embedded Tomcat`.

---

### 20. ¿Cómo sabemos que una aplicación funciona correctamente?

**Tres señales, en orden de profundidad:**

| Señal | Comando | Esperado |
| :--- | :--- | :--- |
| Sonda de salud | `curl <url>/actuator/health` | `{"status":"UP"}` |
| Estado de la revisión | `az containerapp revision list` | `Active` / `Healthy` |
| Evidencia de uso real | métrica `Requests` en Azure Monitor | Mayor que cero |

**El pipeline aplica el mismo criterio:** después de desplegar, consulta el health hasta obtener HTTP 200 y
**solo entonces se declara exitoso**.

---

### 21. ¿Cómo probamos el login mediante Postman?

**Técnica:** dos peticiones encadenadas.

| Paso | Configuración |
| :--- | :--- |
| **1. Login** | `POST {{url}}/api/auth/login` · Body → *raw* → *JSON*: `{"email":"...","password":"..."}` |
| | En la pestaña **Tests**: `pm.collectionVariables.set("token", pm.response.json().token);` |
| **2. Endpoint protegido** | `GET {{url}}/api/auth/me` · Authorization → *Bearer Token* → `{{token}}` |

**Casos que hay que mostrar:**

| Caso | Resultado esperado |
| :--- | :--- |
| Registro | 201 + token |
| Login correcto | 200 + token |
| Login con contraseña incorrecta | **401** |
| Endpoint protegido sin token | **403** |
| Endpoint protegido con token | 200 + datos del usuario |
| Token adulterado | **403** |
| `/actuator/metrics` sin token | **403** |
| `/actuator/health` sin token | 200 (solo el estado) |
| `/actuator/health` con token | 200 (detalle completo) |

**Demostración equivalente sin Postman:**
```bash
TOKEN=$(curl -s -X POST https://<url>/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"vanina.fuentes@um.edu.ar","password":"demo2026unm"}' | jq -r .token)
curl -H "Authorization: Bearer $TOKEN" https://<url>/api/auth/me
```

---

### 22. ¿Cuál es la URL real donde se ejecuta el backend?

```
https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io
```

**De dónde sale:** Azure asigna un dominio al Container App (`<nombre>.<hash-único>.<región>.azurecontainerapps.io`).
Se consulta con:
```bash
az containerapp show -n interview-backend -g rg-interview-app \
  --query "properties.configuration.ingress.fqdn" -o tsv
```

**Portal:** Container Apps → `interview-backend` → **Overview → Application Url**.

---

# PARTE 3 — SEGURIDAD, JWT Y BASE DE DATOS (preguntas 23 a 30)

### 23. ¿Cómo funciona JWT?

**Técnica:** Un JWT tiene tres partes separadas por puntos: `header.payload.signature`.

- **Header:** algoritmo y tipo (`HS384` en este proyecto, `JWT`). El algoritmo se deriva del tamaño de
  la clave: con 48 bytes, jjwt usa HS384; con 32 usaría HS256.
- **Payload:** los *claims* — en nuestro caso `sub` (email), `name`, `id`, `iat` (emitido) y `exp` (expira).
- **Signature:** HMAC-SHA384 sobre el header y el payload, usando una **clave secreta** que solo conoce el servidor.

**Lo importante:** el payload **no está cifrado**, está codificado en Base64. Cualquiera puede leerlo. Lo que
garantiza la firma es que **nadie puede modificarlo** sin la clave secreta.

**En voz alta:** *"El token lleva quién sos y cuándo expira, firmado con una clave secreta del servidor. No
está cifrado: se puede leer, pero no se puede modificar."*

**Demostración:** pegar el token en https://jwt.io y mostrar el payload; después cambiar un carácter y
mostrar que el servidor lo rechaza con 403.

---

### 24. ¿Cómo se validan los tokens?

**Técnica:** `JwtUtil.isTokenValid()` verifica dos cosas: que la **firma** sea correcta (usando la clave
secreta) y que **no haya expirado** (`exp`). Se ejecuta en `JwtAuthFilter`, en cada petición.

**No se consulta la base de datos para validar la firma** — es una verificación criptográfica local, sin
acceso a disco ni a red. La consulta al repositorio es solo para cargar los datos del usuario.

**Demostración:** modificar un carácter **en el medio de la firma** y ver el 403. (Ver la nota de abajo:
agregar caracteres al final no sirve para demostrarlo.)

> **Detalle fino que conviene conocer (y que impresiona en la defensa):** si al token se le **agrega** un
> carácter al final, se obtiene la misma respuesta 200. No es una vulnerabilidad, y conviene entender por qué:
> la firma son 48 bytes codificados en Base64; el decodificador **ignora un carácter sobrante** al final, así
> que el token decodificado son **exactamente los mismos bytes** y la firma sigue siendo válida. Es decir: el
> atacante recibiría el mismo token que ya tenía, sin ningún privilegio extra.
>
> Lo que **sí** se rechaza (verificado, HTTP 403) es cualquier cambio que altere los bytes: modificar un
> carácter en el medio de la firma, modificar el payload, o agregar tres caracteres (que cambian la longitud
> decodificada). **No existe forma de forjar un token nuevo sin la clave.**

**Verificado contra la aplicación real:**

| Token | Resultado |
| :--- | :---: |
| Válido | 200 |
| Válido + 1 carácter al final | 200 (mismos bytes decodificados) |
| Válido + 3 caracteres al final | **403** |
| Carácter cambiado en el medio de la firma | **403** |
| Payload modificado | **403** |

---

### 25. ¿Cómo protegemos los endpoints?

**Técnica:** En `SecurityConfig.securityFilterChain()`, con una regla explícita:

```java
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()          // preflight de CORS
.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
.anyRequest().authenticated()                                    // TODO lo demás exige JWT
```

Más: sin sesiones (`STATELESS`), sin CSRF, y el `JwtAuthFilter` insertado **antes** del filtro de usuario y
contraseña.

**En voz alta:** *"Solo el registro, el login y la salud son públicos. Cualquier otra ruta exige un token
válido."*

---

### 26. ¿Para qué sirve UserDetailsService?

**Técnica:** Es la interfaz de Spring Security que sabe **cómo obtener un usuario**. Nuestra implementación
(declarada como `@Bean` en `SecurityConfig`) busca en la tabla `users` por email y construye un `UserDetails`
con `ROLE_USER`.

Sirve para dos cosas concretas:
1. **Que Spring Security deje de generar la contraseña aleatoria** de desarrollo al arrancar.
2. **Que el `AuthenticationManager` valide las credenciales del login** contra nuestra base de datos.

---

### 27. ¿Para qué sirve HikariCP?

**Técnica:** Es el **pool de conexiones**. Abrir una conexión a MySQL es caro (handshake TCP, autenticación,
negociación SSL). El pool las mantiene abiertas y las reutiliza: cuando una petición necesita la base, pide
una conexión prestada y la devuelve al terminar.

Configuración en `application.properties`, todo parametrizable por variables de entorno:

| Parámetro | Valor | Por qué |
| :--- | :--- | :--- |
| `maximum-pool-size` | 10 | Adecuado para el límite de conexiones del servidor; más conexiones no siempre es mejor |
| `minimum-idle` | 2 | Mantiene conexiones listas para el primer uso |
| `connection-timeout` | 20 s | Cuánto esperar antes de fallar |
| `max-lifetime` | 20 min | Renueva conexiones viejas (evita conexiones TCP "muertas") |
| `leak-detection-threshold` | 30 s | Avisa en el log si alguien olvidó devolver una conexión |

**Demostración:**
```bash
curl -H "Authorization: Bearer $TOKEN" "https://<url>/actuator/metrics/hikaricp.connections.max"
# → {"measurements":[{"statistic":"VALUE","value":10.0}], "availableTags":[{"tag":"pool","values":["InterviewHikariPool"]}]}
```

**Cómo se lee:** si `hikaricp.connections.pending` sube o `hikaricp.connections.timeout` es mayor que cero,
el pool quedó corto y hay que aumentarlo.

---

### 28. ¿Dónde almacenamos las variables de entorno y los secretos?

**Técnica:** en tres lugares, según el entorno, y **nunca en el código**:

| Entorno | Dónde | Qué contiene |
| :--- | :--- | :--- |
| Desarrollo | Archivo `.env` local (ignorado por git) | Contraseña de MySQL local, secreto JWT |
| Plantilla | `.env.example` (versionado) | Nombres y valores de ejemplo, **sin credenciales reales** |
| Producción | **Azure Container App Secrets** | `db-password`, `jwt-secret`, referenciados con `secretref:` |

Además, para las credenciales de GitHub Actions: **GitHub Secrets** (`AZURE_CREDENTIALS`).

**La separación clave:** las credenciales de **GitHub** (para desplegar) son distintas de las de **Spring
Boot** (para ejecutarse). El pipeline nunca ve la contraseña de MySQL y la aplicación nunca ve el secreto
del Service Principal.

---

### 29. ¿Dónde configuramos la conexión a la base de datos?

**Técnica:** En `application.properties`, **parametrizada con marcadores**:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/interviewai_db?...}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}
```

La sintaxis `${VARIABLE:valor_por_defecto}` significa: *"usá la variable de entorno; si no está definida,
usá el valor por defecto (para desarrollo local)"*.

En Azure, el valor real se inyecta como variable de entorno del Container App, y la contraseña apunta a un
secreto (`secretref:db-password`), **no está escrita en ningún lado**.

---

### 30. ¿Cómo verificamos la conectividad con la base de datos?

**Tres niveles, de menor a mayor confianza:**

1. **Sonda de salud:** `GET /actuator/health` con token → `components.db.status = UP`
2. **Consulta SQL directa:**
   ```bash
   docker run --rm -e MYSQL_PWD="..." mysql:8 mysql -h mysql-interview-backend.mysql.database.azure.com \
     -u interviewadmin --ssl-mode=REQUIRED interviewai_db -e "SHOW TABLES;"
   ```
3. **Prueba de escritura real:** registrar un usuario y verificar que aparece en la tabla `users`.

**Evidencia obtenida:** tablas `users`, `questions` e `interview_sessions` creadas por Hibernate, y 10
preguntas sembradas por el `DataSeeder`.

---

# PARTE 4 — VULNERABILIDADES Y ANÁLISIS DE SEGURIDAD (preguntas 31 a 39)

### 31. ¿Qué es un CVE?

**Técnica:** *Common Vulnerabilities and Exposures*. Es un **identificador único y estandarizado** para una
vulnerabilidad públicamente conocida, con formato `CVE-AAAA-NNNNN`. Lo administra MITRE.

**No es una herramienta: es un estándar de nomenclatura.** Existe para que un reporte de Grype, uno de Trivy
y uno de Snyk hablen de **la misma falla** con el mismo nombre.

**Ejemplo del proyecto:** `CVE-2026-5450` (vulnerabilidad en `libc6`, sin corrección publicada).

---

### 32. ¿Qué es CVSS?

**Técnica:** *Common Vulnerability Scoring System*. Es el **puntaje de 0.0 a 10.0** que mide la gravedad
técnica de una vulnerabilidad, calculado a partir de métricas: vector de ataque (red/local), complejidad,
privilegios necesarios, interacción del usuario e impacto en confidencialidad, integridad y disponibilidad.

| Rango | Severidad |
| :--- | :--- |
| 0.0 – 3.9 | Baja |
| 4.0 – 6.9 | Media |
| 7.0 – 8.9 | Alta |
| 9.0 – 10.0 | **Crítica** |

**Tampoco es una herramienta: es un estándar de medición.** Sirve para **priorizar**: no se puede arreglar
todo a la vez, entonces se empieza por lo crítico.

**Cómo lo usamos:** Grype devuelve la severidad y el puntaje; nosotros priorizamos las 10 críticas y después
las 62 altas, y dejamos las *negligible* justificadas.

---

### 33. ¿Cómo utilizamos Grype?

**Técnica:** Grype compara los paquetes de la **imagen Docker construida** (librerías Java, paquetes de
Debian, binarios) contra bases de datos de vulnerabilidades.

```bash
# En Linux/macOS, o en el pipeline:
grype interview-backend:v1.1.0 -o table

# En Windows (bug del proveedor docker: usa ":" en la caché, inválido en NTFS):
MSYS_NO_PATHCONV=1 docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  anchore/grype:latest interview-backend:v1.1.0 -o table
```

**En el pipeline** se ejecuta automáticamente y publica: la tabla completa en el log, el informe JSON como
artefacto descargable y un **resumen por severidad** en el panel de la ejecución.

---

### 34. ¿Qué vulnerabilidades encontramos?

**Escaneo inicial de la imagen `v1.0.0` (186 hallazgos):**

| Severidad | Cantidad |
| :--- | :---: |
| **Critical** | **10** |
| High | 62 |
| Medium | 72 |
| Low | 21 |
| Negligible / Unknown | 21 |

**Las críticas más relevantes:**

| Paquete | Versión | Problema |
| :--- | :--- | :--- |
| `tomcat-embed-core` | 10.1.43 | 6 críticas: *request smuggling*, DoS por streams, bypass de filtros por parsing de URI |
| `spring-security-web` | 6.5.2 | Omisión de reglas de autorización en rutas concurrentes |
| `spring-webmvc` | 6.2.9 | 2 críticas en el manejo de controladores reactivos |
| `libexpat1` | 2.5.0 | Parseo de XML en bibliotecas nativas de Debian |

---

### 35. ¿Cómo las solucionamos?

**Técnica:** actualizando **dentro de la línea compatible**, sin cambiar de tecnología:

| Cambio | De | A | Qué corrigió |
| :--- | :--- | :--- | :--- |
| Spring Boot | 3.5.4 | **3.5.16** | Arrastró Spring Security 6.5.11, Spring Framework 6.2.19, Hibernate 6.6.53 |
| Tomcat (override) | 10.1.43 | **10.1.60** | El BOM fijaba 10.1.55, que dejaba 3 críticas sin resolver |
| Jackson (override) | 2.21.4 | **2.21.7** | El BOM nuevo introducía 11 advisories que la 2.21.7 ya corrige |

**Resultado:** **186 → 115 hallazgos (−38 %)**, críticas **10 → 3**.

**Y lo más importante: se verificó que la aplicación siguiera funcionando** después de actualizar (build con
tests, arranque, login, endpoints, métricas).

---

### 36. ¿Qué hacemos si no tienen solución disponible?

**Técnica:** se documentan y se justifican **caso por caso**, y se revisan en cada re-escaneo. No se
"esconden" ni se fuerza un parche que rompa algo.

**Las 3 críticas que quedan y su justificación:**

| Paquete | Motivo por el que no se corrige |
| :--- | :--- |
| `spring-webmvc` 6.2.19 (2 advisories) | **Es la última versión publicada de la línea 6.2.x** (verificado en Maven Central). No hay corrección sin saltar a Spring Framework 7 / Spring Boot 4, un cambio de versión mayor con incompatibilidades. Además, afectan a controladores **reactivos**, que este proyecto no usa (es una API REST bloqueante). |
| `libc6` (CVE-2026-5450) | Debian ya publicó el parche, pero **Google todavía no reconstruyó la imagen distroless** (verificado: el digest no cambió al hacer `docker pull`). Se mitiga con `USER nonroot`: sin privilegios, la escalada no es explotable. |

**En voz alta:** *"No las escondemos: están documentadas, explicamos por qué no se pueden corregir hoy, qué
mitigación aplicamos y cuándo las vamos a revisar."*

---

### 37. ¿Qué diferencias existen entre Grype y Trivy?

| | **Grype** (Anchore) | **Trivy** (Aqua) |
| :--- | :--- | :--- |
| **Alcance** | Especializado: vulnerabilidades en imágenes y SBOM | Integral: vulnerabilidades, **configuraciones inseguras**, **secretos filtrados**, licencias |
| **Dockerfile / IaC** | No | **Sí** (analiza el Dockerfile, Terraform, Kubernetes) |
| **Detección de secretos** | No | **Sí** (API keys, tokens) |
| **Velocidad** | Muy rápido y liviano | Más pesado por la cantidad de reglas |
| **Uso típico** | *Gate* de vulnerabilidades en el pipeline | Auditoría integral de postura de seguridad |

**En una frase:** *"Grype hace una cosa y la hace rápido; Trivy hace varias y por eso es más pesado. Elegimos
Grype para el pipeline y complementamos con CodeQL, que es el análisis de código que Trivy no hace."*

**Nota:** Trivy **no está instalado** en el proyecto; la comparación es conceptual.

---

### 38. ¿Qué es CodeQL?

**Técnica:** Es el motor de **análisis estático** (SAST) de GitHub. Convierte el código en una base de datos
consultable y ejecuta consultas que buscan **patrones de código inseguro**: inyección SQL, XSS,
deserialización insegura, criptografía débil, exposición de información.

**Punto clave para no equivocarse:** CodeQL analiza **sin ejecutar** la aplicación. **No bloquea ataques en
tiempo de ejecución** — solo advierte que ese patrón existe en el código, *antes* de que se despliegue.

---

### 39. ¿Qué diferencia hay entre CodeQL y Grype?

| | **Grype** | **CodeQL** |
| :--- | :--- | :--- |
| **Qué analiza** | Dependencias e imagen | **Código fuente propio** |
| **Qué detecta** | "Esta biblioteca tiene una CVE publicada" | "Este código nuestro tiene un patrón vulnerable" |
| **Riesgo que cubre** | **Heredado** (de terceros) | **Propio** (lo que escribimos) |
| **Cuándo** | Cuando alguien ya publicó la falla | Antes de que exista un ataque |

**En una frase:** *"Son complementarios: Grype mira lo que trajimos de afuera, CodeQL mira lo que escribimos
nosotros."*

---

# PARTE 5 — MONITOREO Y OBSERVABILIDAD (preguntas 40 a 43)

### 40. ¿Cómo comprobamos que la aplicación recibe peticiones?

**Dos fuentes independientes:**

```bash
# 1. Desde la aplicación (Micrometer)
curl -H "Authorization: Bearer $TOKEN" "https://<url>/actuator/metrics/http.server.requests"
# → COUNT: 12 peticiones

# 2. Desde Azure (sin tocar la aplicación)
az monitor metrics list --resource <id> --metric Requests --interval PT1H --aggregation Total -o table
# → Requests: 2.0
```

**La segunda es la más fuerte para la defensa:** la mide **Azure, desde afuera**, no la aplicación sobre sí
misma. No se puede "trucar" desde el código.

---

### 41. ¿Cómo obtenemos métricas del login?

**Técnica:** `http.server.requests` se etiqueta automáticamente con la URI:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "https://<url>/actuator/metrics/http.server.requests?tag=uri:/api/auth/login"
```

Devuelve **cuántas veces** se llamó al login, cuánto tardaron y cuántas fallaron (la etiqueta `status`
distingue los 200 de los 401).

**Utilidad práctica:** un pico de respuestas **401** en `/api/auth/login` sugiere un ataque de fuerza bruta
o usuarios que no recuerdan su contraseña.

---

### 42. ¿Cómo sabemos cuánto tarda una petición?

**Técnica:** la misma métrica expone tres estadísticos:

| Estadístico | Significado |
| :--- | :--- |
| `COUNT` | Cantidad de peticiones |
| `TOTAL_TIME` | Tiempo acumulado (segundos) |
| `MAX` | **La más lenta** — la que revela los problemas |

**Evidencia real medida:**

| Endpoint | Peticiones | Tiempo total | Más lenta |
| :--- | :---: | :---: | :---: |
| `/api/questions` | 5 | 0,089 s | 0,023 s |
| Global | 12 | 1,034 s | 0,489 s |

**En voz alta:** *"El promedio puede mentir, por eso miramos el máximo: si el máximo se dispara, hay
peticiones que están sufriendo."*

---

### 43. ¿Qué relación existe entre los logs y los 12 factores?

**Técnica:** corresponde al **factor XI — Logs: tratar los logs como flujo de eventos**.

La aplicación **no escribe archivos de log**: escribe a `stdout`/`stderr`, y la plataforma los captura y los
envía a Log Analytics. Ventajas: el contenedor es efímero y no acumula estado; los logs de todas las
réplicas se centralizan; se pueden consultar con KQL.

**Pero cuidado con la trampa:** los logs son **una parte** de la observabilidad, **no toda**.

| Herramienta | Responde a |
| :--- | :--- |
| Logs | *¿Qué pasó?* (el detalle del evento) |
| Métricas | *¿Cuánto, cuántas veces, cuán rápido?* (la tendencia) |
| Sondas de salud | *¿Está vivo y listo ahora mismo?* |

Por eso el proyecto tiene **tres capas** y no solo logs.

**Los otros factores que aplican:**
- **III. Config:** toda la configuración por variables de entorno.
- **V. Build, release, run:** construir, publicar y ejecutar son etapas separadas (y por eso se puede publicar sin liberar).
- **VII. Port binding:** Spring Boot expone el puerto 8080 por sí mismo, sin servidor externo.

---

# PARTE 6 — CONFIGURACIÓN E INFRAESTRUCTURA (preguntas 44 a 47)

### 44. ¿Cómo configuramos desarrollo y producción?

**Técnica:** **el mismo artefacto** (la misma imagen) se ejecuta en los dos entornos; lo único que cambia es
la **configuración inyectada por variables de entorno**.

| Aspecto | Desarrollo | Producción (Azure) |
| :--- | :--- | :--- |
| Base de datos | MySQL en Docker Compose (`db:3306`) | MySQL gestionado de Azure (`...mysql.database.azure.com`) |
| Credenciales | Valores locales en `.env` | Secretos del Container App (`secretref:`) |
| `ddl-auto` | `update` | `update` |
| `show-sql` | `true` | `false` |
| SSL | Deshabilitado | `sslMode=REQUIRED` |
| Secretos | Variables de entorno locales | Container App Secrets |

**En voz alta:** *"La misma imagen corre en los dos lados. Lo que cambia es la configuración que le inyectás
por variables de entorno. Eso es el factor III de los doce factores."*

---

### 45. ¿Cómo podríamos volver a una imagen anterior?

**Técnica:** como todas las versiones quedan en el ACR con su etiqueta, **no hay que reconstruir nada**:

**Opción 1 — Desde GitHub (recomendada):** Actions → *Run workflow* → marcar **deploy** → escribir la
versión anterior (por ejemplo `v1.0.1`) en el campo *version* → *Run workflow*. El pipeline verifica que
exista, la despliega y comprueba el health check.

**Opción 2 — Desde la terminal:**
```bash
az containerapp update -n interview-backend -g rg-interview-app \
  --image acrvaniainterview.azurecr.io/interview-backend:v1.0.1
```

**Opción 3 — Con el script:** `bash scripts/admin.sh` → opción 3 (te lista las versiones disponibles y te
pide confirmación).

**Por qué funciona:** las imágenes son **inmutables** y quedan etiquetadas en el registro. Volver atrás es
simplemente apuntar a otra etiqueta. Ese es el valor de no desplegar nunca `latest`.

---

### 46. ¿Cómo funciona el Dockerfile multietapa?

**Técnica:** dos `FROM` en un mismo archivo. La **primera etapa** (builder) usa una imagen con JDK y Gradle
para compilar y generar el JAR; la **segunda** parte de una base mínima y **copia solo el JAR** desde la
primera:

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS builder     # 1) compila
...
COPY --from=builder /workspace/build/libs/....jar app.jar
FROM gcr.io/distroless/java21-debian12:nonroot AS production   # 2) ejecuta
```

**Resultado:** la imagen final **no contiene** JDK, Gradle, código fuente ni dependencias de compilación.
Pasa de más de 1 GB a **380 MB**, y —más importante— reduce la superficie de ataque.

**Detalle fino para lucirse:** los `COPY` copian primero los archivos de build y después el código, para
aprovechar la **caché de capas**: si no cambian las dependencias, no se vuelven a descargar.

📄 Explicación línea por línea en **[docker.md](docker.md)**

---

### 47. ¿Qué diferencia existe entre Dockerfile y Docker Compose?

| | **Dockerfile** | **Docker Compose** |
| :--- | :--- | :--- |
| **Define** | Cómo se **construye una imagen** | Cómo se **ejecutan los contenedores** |
| **Responde** | "¿Qué contiene y cómo arranca mi aplicación?" | "¿Qué levanto, cómo se conecta y con qué configuración?" |
| **Comando** | `docker build` | `docker compose up` |
| **En el proyecto** | Construye la imagen del backend | Levanta el backend **+ MySQL** con red, volumen y healthcheck |

**En una frase:** *"El Dockerfile construye la imagen; Docker Compose la ejecuta junto a la base de datos."*

---

# PARTE 7 — INTEGRACIÓN Y CIERRE (preguntas 48 a 50)

### 48. ¿Cómo se conecta React Native con Spring Boot desplegado en Azure?

**Técnica:** el cliente apunta la URL base al dominio HTTPS de Azure. En el frontend está centralizado en
`src/constants/api.ts`:

```typescript
export const API_BASE_URL = getApiBaseUrl();   // hoy apunta a localhost / IP local
// En producción: 'https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io'
```

El resto del flujo ya funciona: el login devuelve el JWT, se guarda en `AsyncStorage` y se envía en el header
`Authorization: Bearer` de cada petición.

**Aclaración de alcance:** la integración con el cliente móvil **quedó fuera del alcance acordado** de este
trabajo. Lo que se despliega en Azure es el backend; la aplicación es un cliente que **no requería
modificaciones** para el despliegue. El cambio necesario es de configuración (una constante), no de lógica.

**Si el profesor pidiera la demo:** cambiar esa constante, ejecutar `npm start` y mostrar el login desde el
celular contra la URL de Azure.

---

### 49. ¿Cómo comprobamos que el backend usa credenciales reales y no datos hardcodeados?

**Cinco comprobaciones, todas verificables:**

1. **El repositorio es público y no contiene secretos.** Cualquiera puede revisarlo:
   ```bash
   grep -rn "password\|secret\|api_key" src/ --include=*.java --include=*.properties
   # Solo aparecen marcadores ${VARIABLE:default} y comentarios
   ```

2. **`.gitignore` excluye `.env` y `.secrets/`**, y `.dockerignore` los excluye del contexto de build.

3. **`application.properties` usa la sintaxis `${VARIABLE:default}`** — el valor real nunca está en el archivo.

4. **En Azure los valores apuntan a secretos**, no a texto plano:
   ```bash
   az containerapp show -n interview-backend -g rg-interview-app \
     --query "properties.template.containers[0].env[].name" -o tsv
   # SPRING_DATASOURCE_PASSWORD y JWT_SECRET están como secretref
   ```

5. **La prueba de fuego:** si las credenciales no llegaran de verdad, la aplicación **no conectaría a la
   base**. Y conecta: `db: {"status":"UP","database":"MySQL"}`. Es imposible que eso funcione con valores
   hardcodeados de desarrollo, porque el servidor de Azure no acepta esas credenciales.

**En voz alta:** *"El repositorio es público: si hubiera una credencial, la verías. Y la prueba definitiva es
que la aplicación conecta a una base que solo acepta las credenciales que le inyectamos por variables de
entorno."*

---

### 50. ¿Qué haríamos si el pipeline falla durante la construcción, el análisis de seguridad o el despliegue?

**Técnica:** el pipeline **falla temprano y a propósito**: cada etapa verifica antes de pasar a la siguiente,
así nunca se publica algo roto. El procedimiento es siempre el mismo:

**1. Leer el log e identificar el paso rojo** (Actions → la ejecución → el paso marcado en rojo).

**2. Según la etapa:**

| Etapa que falló | Causas típicas | Qué hacer |
| :--- | :--- | :--- |
| **Compilación / tests** | Error de código, test roto, `gradlew` sin permiso de ejecución | Reproducir en local con `./gradlew build`; el log indica el archivo y la línea |
| **Grype** | Vulnerabilidad crítica nueva (si el *gate* estuviera activo) | Revisar el informe, evaluar actualización; hoy **no rompe** el pipeline, solo informa |
| **Login en Azure** | Secreto `AZURE_CREDENTIALS` vencido o mal cargado | Regenerar la credencial del Service Principal y actualizar el secret |
| **Push al ACR** | Permisos del Service Principal (falta `AcrPush`), registro inaccesible | Verificar con `az role assignment list` |
| **Despliegue** | Permisos sobre el Container App, imagen inexistente | El paso *"Verificar que la imagen existe"* lo detecta antes |
| **Verificación (health check)** | La aplicación no arranca — **típicamente, la base de datos está apagada** | Encender la base, esperar `Ready`, reintentar con *Re-run all jobs* |

**3. Reintentar:** botón **Re-run all jobs** (o *Re-run failed jobs*).

**Caso real que ya nos pasó:** la **primera** ejecución del pipeline falló en la compilación. El log mostró
`Permission denied` al ejecutar `./gradlew`. La causa no era el código: en Windows, git no registra el bit de
ejecución, así que el archivo había quedado guardado como no ejecutable. Se corrigió con
`git update-index --chmod=+x gradlew`, que arregla el permiso **en el repositorio** (y no solo en el pipeline).

**En voz alta:** *"El pipeline está diseñado para fallar temprano: si los tests fallan, ni siquiera se
construye la imagen. Después se lee el log, se identifica la etapa y se corrige. Ya lo hicimos una vez."*

---
---

# 🎯 SIMULACRO DE DEFENSA

## ESCENARIO 1 — *"Hacé un cambio, subilo, construí una imagen versionada, publicala, pero NO hagas deploy"*

**Este es el Caso A, y ya está verificado** (ejecución 38182406341).

### Paso a paso

```bash
# 1. Hacer un cambio cualquiera (por ejemplo, en el README o un comentario)
cd "c:/Users/vanin/Documents/Interview-backend_prats"

# 2. Commit y push
git add -A
git commit -m "docs: cambio de demostracion para el parcial"
git push origin main
```

**Lo que se dice mientras corre:** *"Estoy haciendo un push a main. El pipeline se dispara solo: compila,
corre los tests, construye la imagen, la escanea con Grype y la publica en el registro. **No va a desplegar,
porque un push a main no cumple ninguna condición de liberación.**"*

### Cómo se demuestra que NO desplegó

1. Ir a **Actions** → la ejecución en curso
2. El job **"Desplegar en Azure Container Apps"** aparece como **skipped** (ícono gris)
3. Verificar que la versión en Azure **no cambió**:
   ```bash
   az containerapp show -n interview-backend -g rg-interview-app \
     --query "properties.template.containers[0].image" -o tsv
   # Sigue siendo v1.1.0
   ```
4. Verificar que **sí** se publicó la imagen nueva:
   ```bash
   az acr repository show-tags --name acrvaniainterview --repository interview-backend -o tsv
   # Aparece la etiqueta nueva con el SHA del commit
   ```

**Frase de cierre:** *"La imagen quedó publicada y escaneada, pero la aplicación sigue corriendo la versión
anterior. Esa es la diferencia entre construir y liberar."*

---

## ESCENARIO 2 — *"Ahora desplegá esa misma versión, comprobá que funciona, mostrame los logs y detené la aplicación"*

### 2.a — Desplegar esa misma versión

**Antes de nada:** verificar que la base esté encendida.
```bash
az mysql flexible-server list -o table     # tiene que decir "Ready"
```

**Opción recomendada (desde GitHub):**
Actions → *Run workflow* → **marcar la casilla "deploy"** → dejar *version* vacío (o escribir la etiqueta) →
*Run workflow*.

**Opción por terminal:**
```bash
az containerapp update -n interview-backend -g rg-interview-app \
  --image acrvaniainterview.azurecr.io/interview-backend:v1.1.0
```

### 2.b — Comprobar que funciona

```bash
# 1. Estado general
bash scripts/admin.sh estado

# 2. Health check público
curl https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io/actuator/health
# → {"status":"UP","groups":["liveness","readiness"]}

# 3. Que la base esté conectada (necesita token)
TOKEN=$(curl -s -X POST https://<url>/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"vanina.fuentes@um.edu.ar","password":"demo2026unm"}' | jq -r .token)
curl -H "Authorization: Bearer $TOKEN" https://<url>/actuator/health
# → "db":{"status":"UP","database":"MySQL"}
```

**En voz alta:** *"El primer health me dice que la aplicación está viva. El segundo, con token, me confirma
que además está conectada a la base de datos."*

### 2.c — Mostrar los logs

**Opción 1 — En vivo (portal, la más vistosa):**
Container Apps → `interview-backend` → **Monitoring → Log stream** → hacer una petición y ver las líneas
aparecer.

**Opción 2 — Por terminal:**
```bash
az containerapp logs show -n interview-backend -g rg-interview-app --tail 30
```

**Opción 3 — Históricos (si está en 0 réplicas):**
```bash
bash scripts/admin.sh    # opción 7
```

**Para que aparezca algo interesante**, provocar un error a propósito *antes*:
```bash
curl -X POST https://<url>/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"noexiste@test.com","password":"malaclave"}'
# → 401, y queda registrado en los logs
```

### 2.d — Detener la aplicación

**Decir esto antes de hacerlo** (demuestra criterio, no solo mecánica):

> *"Acá hay que tener cuidado. La aplicación ya está configurada para escalar a cero réplicas, así que no
> consume nada cuando no hay tráfico — no hace falta detenerla. Y detener el Container App tiene un riesgo:
> si el entorno queda sin ninguna aplicación, Azure puede marcarlo para auto-borrado, lo que rompería el
> próximo despliegue. **Por eso lo que conviene detener es la base de datos**, que es la que sí genera
> costo."*

**Detener la base (lo correcto):**
```bash
az mysql flexible-server stop -g rg-interview-app -n mysql-interview-backend
# Portal: Azure Database for MySQL flexible servers → mysql-interview-backend → Stop
```

**Si el profesor insiste en detener la aplicación:**
```bash
az containerapp stop -n interview-backend -g rg-interview-app
# Para volver a levantarla:
az containerapp start -n interview-backend -g rg-interview-app
```

### 2.e — Cierre

**Frase final:** *"El ciclo completo es: cambio → push → pipeline automático → imagen versionada y escaneada
→ despliegue condicionado → verificación con health check → operación y monitoreo. Todo trazable: desde el
contenedor que está corriendo en Azure se puede llegar hasta el commit exacto que lo generó."*

---

# 📋 CHULETA RÁPIDA

## Comandos que hay que tener a mano

```bash
# Estado general
bash scripts/admin.sh estado

# Encender/apagar la base
az mysql flexible-server start|stop -g rg-interview-app -n mysql-interview-backend
az mysql flexible-server list -o table

# Qué versión corre en Azure
az containerapp show -n interview-backend -g rg-interview-app \
  --query "properties.template.containers[0].image" -o tsv

# Imágenes publicadas
az acr repository show-tags --name acrvaniainterview --repository interview-backend -o tsv

# Logs
az containerapp logs show -n interview-backend -g rg-interview-app --tail 30

# Health
curl https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io/actuator/health

# Desplegar una versión anterior
az containerapp update -n interview-backend -g rg-interview-app \
  --image acrvaniainterview.azurecr.io/interview-backend:v1.0.1
```

## Números que conviene recordar

| Dato | Valor |
| :--- | :--- |
| Versión desplegada | `v1.1.0` |
| Vulnerabilidades | **186 → 115** (−38 %) · críticas **10 → 3** |
| Pasos del Dockerfile | 2 etapas (builder + distroless) |
| Tamaño de la imagen | 380 MB (119 MB comprimida) |
| Pool de conexiones | 10 conexiones, `InterviewHikariPool` |
| Réplicas | 0 a 1 (escala a cero) |
| Costo esperado | ~USD 8-10/mes con la base apagada entre sesiones |
| Etiquetas de imagen | `:v1.1.0` (versión) y `:<sha>` (commit) |

## Las tres frases que resumen el proyecto

1. **"La construcción de imágenes es exclusiva del pipeline"** — corregimos el problema del proyecto de
   referencia, donde el script manual y el CI podían desplegar imágenes distintas.
2. **"Publicar no es liberar"** — se puede construir y publicar sin desplegar, y liberar solo cuando se
   decide.
3. **"El pipeline verifica que la aplicación responda de verdad"** — no se declara exitoso un despliegue
   hasta que el health check devuelve HTTP 200.

---
---

# 📋 ANEXO — CRUCE CON EL CHECKLIST DE LA CLASE

Verificación de cada punto del checklist de revisión técnica contra el código real del proyecto.

**Leyenda:** ✅ Cumple · ➖ No aplica (justificado) · 🔧 Completado en esta revisión

---

## 1. Clase principal — `InterviewBackApplication.java` ✅

| Punto | Dónde |
| :--- | :--- |
| Punto de entrada con `@SpringBootApplication` y `main()` | `InterviewBackApplication.java` |
| Cómo Spring inicia el `ApplicationContext` | El `main()` llama a `SpringApplication.run()`, que crea el contexto, **escanea el paquete base** y registra los componentes |
| `@Configuration`, `@Bean`, `@Component`, `@Service`, `@Autowired` | Ver el cuadro de abajo |
| Inyección de dependencias (no usar `new`) | Todos los servicios y controladores reciben sus dependencias por constructor |

**Qué significa cada anotación en este proyecto:**

| Anotación | Dónde se usa | Qué hace |
| :--- | :--- | :--- |
| `@SpringBootApplication` | `InterviewBackApplication` | Combina `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| `@Configuration` | `SecurityConfig`, `CorsConfig`, `AppConfig` | Declara una clase que define beans |
| `@Bean` | `PasswordEncoder`, `UserDetailsService`, `RestTemplate`… | Registra un objeto para que Spring lo gestione |
| `@Service` | `AuthService`, `QuestionService`, `InterviewService`, `GroqService` | Componente de la capa de negocio |
| `@RestController` | `AuthController`, `QuestionController`, `InterviewController` | Componente web que devuelve JSON |
| `@Repository` | Los tres repositorios | Componente de acceso a datos |
| `@Component` | `JwtUtil`, `JwtAuthFilter` | Componente genérico |
| `@Autowired` | **No se usa** — ver abajo | |

**Por qué no aparece `@Autowired`:** con **un solo constructor**, Spring lo usa automáticamente para inyectar.
Escribirlo sería redundante. Se prefiere además la inyección por constructor sobre la de campo porque las
dependencias quedan `final` (inmutables), son visibles en la firma y la clase se puede instanciar en un test
sin levantar Spring.

---

## 2. Configuración general y beans ✅

Clases `@Configuration`: `SecurityConfig`, `CorsConfig`, `AppConfig`.

| Punto | Respuesta |
| :--- | :--- |
| Beans reutilizables con `@Bean`, especialmente `PasswordEncoder` | `SecurityConfig.passwordEncoder()` devuelve un `BCryptPasswordEncoder` |
| Diferencia entre registrar un bean e instanciar un objeto | Un **bean** lo crea y administra Spring: una sola instancia compartida (singleton). Un `new` crea un objeto que nadie más conoce ni puede reemplazar |
| Alcance singleton por defecto | Sí: `PasswordEncoder`, `RestTemplate` y `UserDetailsService` son **una sola instancia** para toda la aplicación |

**Por qué importa que `PasswordEncoder` sea un bean:** `AuthService` lo recibe por constructor y Spring le
entrega **la misma instancia** que usa `SecurityConfig` para el login. Si cada uno hiciera `new
BCryptPasswordEncoder()`, podrían configurarse distinto y el login fallaría.

---

## 3. Servicios e inyección de dependencias ✅

| Punto | Dónde |
| :--- | :--- |
| Servicios con `@Service` | Los cuatro de `service/` |
| Inyección de repositorios y otros servicios por constructor | `AuthService` recibe `UserRepository`, `PasswordEncoder` y `JwtUtil` |
| Evitar `new` en los controladores | Ningún controlador crea servicios con `new` |
| `@Autowired`, inyección por constructor y atributos `final` | Campos `private final`; sin `@Autowired` (constructor único) |
| Lombok | **No se usa** ➖ — los DTOs son `record` de Java (inmutables y con menos código que Lombok). No hace falta agregarlo |

---

## 4. Servicio de usuarios y `UserDetailsService` ✅

| Punto | Dónde |
| :--- | :--- |
| Servicio de usuarios conectado con Spring Security | `SecurityConfig`, bean `userDetailsService` |
| `UserDetailsService` o mecanismo equivalente | Se implementa como **lambda**: `email -> userRepository.findByEmail(email)...` |
| Recuperar usuarios desde la base | `UserRepository.findByEmail()` |
| `loadUserByUsername()` | Es el método de la interfaz; acá la lambda **es** esa implementación |
| Qué pasa si el usuario no existe | Se lanza `UsernameNotFoundException` → el login responde **401** con un mensaje genérico (no revela si el email existe) |

---

## 5. Entidad `User` y seguridad de cuentas ✅ ➖

| Punto | Estado |
| :--- | :--- |
| Atributos de usuario y contraseña | ✅ `id`, `name`, `email`, `password` |
| Guardar contraseñas hasheadas | ✅ BCrypt, con sal automática (`PasswordEncoder.encode()`) |
| `UserDetails`: ¿lo implementa la entidad? | ➖ **No, y no hace falta.** El propio material de la cátedra aclara que puede usarse un adaptador. Acá el adaptador es la lambda del `UserDetailsService`, y en las peticiones autenticadas el principal es `AuthenticatedUser` |
| Estados de cuenta (habilitada, no bloqueada, no expirada) | ➖ **No aplica**: el sistema no tiene bloqueo ni vencimiento de cuentas. Spring los asume habilitados por defecto |
| Métodos requeridos y `@Override` | ✅ `@Override` en `doFilterInternal`, `addCorsMappings` |
| Roles solo si el sistema necesita distintos accesos | ➖ **No los necesita.** Existe un único tipo de usuario, por eso las reglas exigen *estar autenticado* y no un rol. El `ROLE_USER` que se asigna es uniforme y sirve para que Spring construya el usuario y para restringir el detalle del health |

---

## 6. Entidades, JPA e Hibernate ✅

| Punto | Dónde |
| :--- | :--- |
| `@Entity` | `User`, `Question`, `InterviewSession` |
| `@Table` y `@Column` | `@Table(name = "users")`, `@Column(nullable = false, unique = true)` |
| `@Id` y generación de identificadores | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `IDENTITY` vs secuencias | `IDENTITY` delega en el `AUTO_INCREMENT` de MySQL: válido y lo más simple para este motor. Las secuencias aplican a bases como PostgreSQL u Oracle |
| JPA vs Spring Data JPA vs Hibernate | Ver el cuadro de abajo |
| Actualización del esquema | `spring.jpa.hibernate.ddl-auto=update` — Hibernate compara las entidades con el esquema y agrega lo que falta al arrancar |

| Concepto | Qué es |
| :--- | :--- |
| **JPA** | La **especificación** (el conjunto de reglas e interfaces) |
| **Hibernate** | La **implementación** de esa especificación (el ORM que hace el trabajo) |
| **Spring Data JPA** | Una **capa sobre JPA** que genera los repositorios: se declara la interfaz y no se escribe la implementación |

---

## 7. Repositorios y operaciones de base de datos ✅ 🔧

| Punto | Dónde |
| :--- | :--- |
| Repositorios Spring Data JPA | Los tres, extendiendo `JpaRepository` |
| Búsquedas necesarias | `findByEmail`, `findByUserIdOrderByAnsweredAtDesc`, `countByUserIdAndQuestionId` |
| Obtener, guardar y actualizar entidades | `save()`, `findById()`, `findAll()` heredados |
| `@Transactional` y `readOnly = true` | 🔧 **Agregado en esta revisión** |
| Diferencia entre lectura y transacción que modifica | Ver abajo |

**Dónde se aplicó `@Transactional`:**

| Método | Anotación | Por qué |
| :--- | :--- | :--- |
| `AuthService.register` | `@Transactional` | Verifica si el email existe **y después** inserta: dentro de una transacción, dos registros simultáneos con el mismo email no pueden superar la verificación a la vez |
| `AuthService.login` | `readOnly = true` | Solo lee |
| `QuestionService.getAllQuestions` / `getQuestionById` | `readOnly = true` | Solo leen |
| `QuestionService.createQuestion` | `@Transactional` | Escribe |
| `InterviewService.getUserHistory` / `getUserStats` | `readOnly = true` | Solo leen |
| `InterviewService.evaluate` / `getHint` | **Sin anotación, a propósito** | Llaman a la API de IA (lenta y externa). Mantener una transacción abierta durante esa llamada retendría una conexión del pool sin usarla; con un pool de 10 conexiones, alcanzaría para agotarlo. Cada operación de base se ejecuta en su propia transacción |

> ⚠️ **CORRECCIÓN IMPORTANTE —`readOnly = true` NO evita inyección SQL.**
>
> Es una **optimización**, no una defensa. Le indica a Hibernate que la transacción no va a modificar datos,
> con dos efectos: **omite el *dirty checking*** (no compara cada entidad cargada contra su estado original
> al terminar) y permite que el driver o la base deriven la operación a una **réplica de lectura**.
>
> Quien evita la inyección SQL es usar **consultas parametrizadas**. En este proyecto eso lo garantiza
> **Spring Data JPA / Hibernate**: `findByEmail(email)` no concatena el valor en la consulta, sino que lo
> envía como **parámetro** al `PreparedStatement` del driver. Por eso un email como
> `' OR 1=1 --` se busca como texto literal y no altera la consulta.

---

## 8. Configuración de Spring Security ✅

| Punto | Dónde |
| :--- | :--- |
| `SecurityFilterChain` | `SecurityConfig.securityFilterChain()` |
| Endpoints públicos y protegidos | `.requestMatchers(...).permitAll()` + `.anyRequest().authenticated()` |
| `AuthenticationProvider` | Bean `authenticationProvider`, un `DaoAuthenticationProvider` con nuestro `UserDetailsService` |
| `PasswordEncoder` y mecanismo de autenticación | `BCryptPasswordEncoder`; el login usa el `AuthenticationManager` |
| Filtro JWT en la cadena | `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` |
| **Autenticación vs autorización** | Ver abajo |

> **Autenticación** responde *"¿quién sos?"* → la resuelve `JwtAuthFilter` validando el token.
> **Autorización** responde *"¿podés hacer esto?"* → la resuelve `SecurityConfig` con las reglas de acceso.
> Están separadas a propósito: el filtro **no rechaza** nada, solo autentica; el rechazo (403) lo decide la
> configuración en un único lugar.

---

## 9. CORS ✅ 🔧

| Punto | Estado |
| :--- | :--- |
| Orígenes autorizados | ✅ Configurables con la variable `CORS_ALLOWED_ORIGINS` |
| Métodos HTTP permitidos | ✅ `GET, POST, PUT, DELETE, OPTIONS` |
| Cabeceras, especialmente `Authorization` | ✅ permitidas |
| Preflight `OPTIONS` | ✅ `SecurityConfig` exime `OPTIONS` de autenticación; sin eso, el navegador recibiría 403 en el preflight y nunca haría la petición real |
| Evitar configuraciones demasiado permisivas en producción | 🔧 Se hizo configurable. **Justificación de por qué `*` es aceptable acá:** el riesgo que CORS previene es que un sitio ajeno haga peticiones **aprovechando las cookies del navegador**. Esta API no usa cookies: el token viaja en la cabecera `Authorization`, que un sitio externo no puede agregar. Al desplegar un cliente web, alcanza con setear la variable |

---

## 10. Servicio de JWT ✅ 🔧

| Punto | Dónde / Respuesta |
| :--- | :--- |
| Generar el JWT tras autenticar | `JwtUtil.generateToken()`, invocado desde `AuthService` |
| Firmar con una clave gestionada de forma segura | Clave en `application.properties` como `${JWT_SECRET:...}`, inyectada desde el secreto del Container App en producción. **Nunca en el código** |
| Validar firma, expiración y claims | `isTokenValid()` → `verifyWith(clave)` valida la firma y `parseSignedClaims` rechaza si expiró |
| `sub`, `iat`, `exp` | `sub` = email, `iat` = emisión, `exp` = expiración (7 días) |
| `audience` | ➖ **No se usa.** El material de la cátedra lo explica pero no confirma que sea obligatorio. Tendría sentido si la API fuera consumida por varias aplicaciones distintas y se quisiera restringir el token a una |
| Dependencia y versión | `io.jsonwebtoken:jjwt-api / impl / jackson` **0.12.6** |
| Identificar al usuario sin datos sensibles | El payload lleva email, nombre e id. **No lleva la contraseña ni el hash** |

**Nota sobre el algoritmo:** es **HMAC-SHA**. El algoritmo exacto lo elige jjwt según el tamaño de la clave:
con la clave actual de 48 bytes usa **HS384**; con una de 32 bytes usaría HS256. Verificado decodificando el
header de un token real.

---

## 11. Filtro de autenticación JWT ✅

| Punto | Dónde |
| :--- | :--- |
| Interceptar las peticiones | `OncePerRequestFilter` (garantiza una sola ejecución por petición) |
| Obtener el token del header | `extraerToken()` lee `Authorization: Bearer ` |
| Validar antes de considerar autenticada | `jwtUtil.isTokenValid(token)` |
| Recuperar la identidad y cargar el usuario | `userRepository.findByEmail()` |
| Registrar en `SecurityContextHolder` | `SecurityContextHolder.getContext().setAuthentication(...)` |
| Token ausente, inválido o vencido | En los tres casos **deja pasar la petición sin autenticar**, y `SecurityConfig` responde **403** |

---

## 12. Controlador de autenticación ✅

| Punto | Dónde |
| :--- | :--- |
| Login que consulta las credenciales | `AuthService.login()` → `findByEmail` + `passwordEncoder.matches()` |
| Devolver un JWT tras autenticar | `AuthResponse` con el token (201 en registro, 200 en login) |
| Endpoints públicos y resto protegido | `SecurityConfig` |
| Recuperar el usuario autenticado | `GET /api/auth/me` con `@AuthenticationPrincipal` |
| Comprobar la validez del token desde el cliente | `/api/auth/me`: si responde 200, el token sigue vigente |
| Códigos HTTP | **201** registro · **200** login y consultas · **401** credenciales inválidas · **403** sin token o token inválido · **409** email ya registrado · **404** recurso inexistente |

---

## 16. Configuración centralizada ✅

Un solo archivo, `application.properties`, con todo lo pedido:

| Punto | Propiedades |
| :--- | :--- |
| URL, usuario y contraseña de la base por variables de entorno | `spring.datasource.url / username / password` con `${VARIABLE:default}` |
| Propiedades de JPA/Hibernate y gestión del esquema | `ddl-auto`, `show-sql`, `open-in-view=false` |
| Propiedades del JWT | `jwt.secret`, `jwt.expiration-ms` |
| Parámetros del pool | `spring.datasource.hikari.*` (6 parámetros) |
| Salud, métricas y observabilidad | `management.endpoint.*`, `management.metrics.tags.*` |
| **Sin secretos reales en archivos versionados** | El repositorio es **público**: solo hay marcadores. Los valores reales viven en los secretos del Container App |

➖ **Perfiles (`application-dev` / `application-prod`):** no se usan. La diferencia entre entornos se resuelve
con **variables de entorno**, que es el enfoque de los 12 factores y evita duplicar archivos de configuración.

---

## 17. Pool de conexiones HikariCP ✅

| Punto | Valor configurado |
| :--- | :--- |
| Pool para reutilizar conexiones JDBC | `pool-name: InterviewHikariPool` |
| `maximum-pool-size` | 10 |
| `minimum-idle` | 2 |
| Timeouts y vida máxima | `connection-timeout: 20000` · `idle-timeout: 300000` · `max-lifetime: 1200000` |
| Verificar que funcionen y se recuperen | `leak-detection-threshold: 30000` avisa en el log si una conexión no se devuelve. Verificado con `hikaricp.connections.max` = 10 |
| **Impacto del número de réplicas** | Ver abajo |
| Para qué sirve y dónde se configura | Ver abajo |

> **El punto de las réplicas (pregunta típica):** el pool es **por instancia** de la aplicación, no global.
> Con `maximum-pool-size = 10` y **1 réplica**, el máximo de conexiones a MySQL es 10. Si el Container App
> escalara a **3 réplicas**, serían **30 conexiones simultáneas**. Por eso el tamaño del pool debe elegirse
> pensando en el peor caso de escalado y en el límite de conexiones del servidor de base de datos. Acá el
> máximo de réplicas está fijado en 1, así que el techo real es de 10 conexiones.

> **Para qué sirve:** abrir una conexión a la base es costoso (handshake TCP, autenticación, negociación
> TLS). El pool las mantiene abiertas y las **reutiliza**, en lugar de abrir y cerrar una por cada petición.

---

## 18. Health checks y métricas ✅

| Punto | Dónde |
| :--- | :--- |
| Mecanismo para comprobar que funciona | `GET /actuator/health` |
| Exponer el endpoint de forma controlada | Es público, pero **sin token muestra solo `{"status":"UP"}`**; el detalle requiere autenticación |
| Comprobar conectividad con la base | `components.db.status = UP` (visible con token), verificado en local y en Azure |
| Información de recursos, memoria y métricas | `GET /actuator/metrics` (98 métricas) y Azure Monitor |
| **Por qué una aplicación iniciada no está lista** | Ver abajo |

> **Iniciada ≠ lista:** Tomcat puede estar escuchando en el puerto mientras la aplicación **todavía no puede
> atender peticiones**, porque sigue inicializando beans. Ese es exactamente el error que sufrimos: Azure
> respondía **503 "App port is not ready"** y los logs mostraban que Spring moría al crear el
> `EntityManagerFactory` por no poder conectarse a la base. Por eso existen **dos sondas distintas**:
> *liveness* (¿el proceso está vivo?) y *readiness* (¿puede recibir tráfico?). El pipeline aplica el mismo
> criterio: no se declara exitoso hasta que el health devuelve HTTP 200.

---

## 19. Dependencias y compilación ✅

| Punto | Respuesta |
| :--- | :--- |
| Gestor elegido | **Gradle** con Kotlin DSL (`build.gradle.kts`) |
| Dependencias compatibles | Spring Boot **3.5.16** gestiona las versiones por BOM (Web, Data JPA, Security, Actuator, Validation) + `jjwt 0.12.6` + `mysql-connector-j` |
| Versión de Java | **21** con `toolchain` y `options.release = 21` (bytecode compatible con el runtime del contenedor) |
| Generar el `.jar` ejecutable | `./gradlew bootJar` |
| Ubicación del artefacto | **`build/libs/interview-back-0.0.1-SNAPSHOT.jar`** (en Maven sería `target/`) |
| Qué ocurre al compilar y al ejecutar pruebas | `compileJava` → bytecode; `test` → levanta el contexto de Spring **contra una base real**, y `bootJar` empaqueta el JAR con Tomcat embebido |

---

## 20. Dockerfile y ejecución ✅ (7/7)

| Punto | Cómo se cumple |
| :--- | :--- |
| Etapas de construcción y ejecución | Dos etapas: `builder` (JDK + Gradle) y `production` |
| Compilar con JDK y gestor de build | `eclipse-temurin:21-jdk-jammy` + `./gradlew bootJar` |
| Copiar solo el artefacto necesario | `COPY --from=builder /workspace/build/libs/....jar app.jar` |
| Versión de Java que ejecute el bytecode | Runtime `java21-debian12` — coherente con el `release = 21` |
| Imagen final mínima | **Distroless**: sin shell, sin `apt`, sin compilador |
| Usuario no privilegiado | `USER nonroot:nonroot` |
| Por qué multietapa y cómo se genera el JAR | Ver `docs/docker.md` |

---

## 21. Pruebas durante la construcción ✅

| Punto | Respuesta |
| :--- | :--- |
| En qué etapa se ejecutan | En **CI**, paso *"Compilar y ejecutar los tests"*, con un **MySQL efímero** que GitHub levanta como *service container* |
| Fallo de compilación vs de tests | La compilación falla por errores de sintaxis o tipos; los tests fallan por comportamiento. **Los dos detienen el pipeline**, pero en pasos distintos |
| Por qué una prueba de integración puede fallar sin base de datos | **Lo vivimos**: `contextLoads` levanta el contexto completo, incluida la conexión a MySQL. Sin base, falla con `Communications link failure`. Pasó localmente y también en la primera ejecución del pipeline (que falló por otro motivo, el permiso de `gradlew`) |
| Consecuencias de omitir pruebas | Se publica una imagen que nunca se verificó. **Nunca se hizo**: los tests corren siempre y si fallan, el pipeline se detiene **antes** de construir la imagen |
| Mantener pruebas funcionales en lugar de eliminarlas | ✅ Los tests se **repararon, no se eliminaron**: cuando faltó la base en CI se agregó el service container; cuando faltó el permiso de ejecución de `gradlew`, se corrigió el modo en git |

### Dos decisiones conscientes sobre las pruebas

1. **`docker build` usa `-x test`**: los tests **no se saltean del proyecto**, se ejecutan en CI. Volver a
   correrlos durante el empaquetado repetiría el mismo trabajo y exigiría una base de datos durante el build.
   Se ejecutan **una vez, en el lugar correcto**.
2. **El pipeline sí usa `./gradlew build`** (con tests). Si se hubieran desactivado para "que quede verde",
   el pipeline no probaría nada — y eso es justamente lo que hay que evitar.

---

## RESUMEN DEL CRUCE

| Ítem del checklist | Estado |
| :--- | :---: |
| 1. Clase principal | ✅ |
| 2. Configuración y beans | ✅ |
| 3. Servicios e inyección | ✅ |
| 4. UserDetailsService | ✅ |
| 5. Entidad User y seguridad de cuentas | ✅ (2 puntos no aplican, justificados) |
| 6. Entidades, JPA e Hibernate | ✅ |
| 7. Repositorios y transacciones | ✅ 🔧 (se agregó `@Transactional`) |
| 8. Configuración de Spring Security | ✅ |
| 9. CORS | ✅ 🔧 (se hizo configurable) |
| 10. Servicio de JWT | ✅ 🔧 (se corrigió el algoritmo documentado) |
| 11. Filtro JWT | ✅ |
| 12. Controlador de autenticación | ✅ |
| 16. Configuración centralizada | ✅ |
| 17. HikariCP | ✅ |
| 18. Health checks y métricas | ✅ |
| 19. Dependencias y compilación | ✅ |
| 20. Dockerfile y ejecución | ✅ |
| 21. Pruebas durante la construcción | ✅ |

**Lo que NO se agregó, a propósito:** Redis, MapStruct, refresh tokens, «Recordarme», `audience` en el JWT,
roles diferenciados, migraciones Flyway y perfiles de Spring. Ninguno es necesario para este sistema y el
material de la cátedra aclara que son herramientas que se usan **cuando la aplicación las necesita**.
