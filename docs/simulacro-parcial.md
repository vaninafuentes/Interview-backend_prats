# Simulacro del parcial — guía paso a paso

Esta es la guía para **practicar la demostración completa**: hacer un cambio, verlo recorrer el
pipeline, desplegarlo, probarlo y controlarlo.

Está escrita para seguirla con el navegador y la terminal abiertos. Cada caso dice **dónde
hacer clic**, **qué comando escribir** y **qué tenés que ver** para saber que salió bien.

---

## Índice

| # | Caso | Qué demuestra |
| :---: | :--- | :--- |
| [0](#caso-0--preparación-hacelo-antes-de-todo) | Preparación | Encender la base, tener todo listo |
| [1](#caso-1--cambio--push--el-pipeline-sin-desplegar) | Cambio + push | Integración Continua: publica, **no** despliega |
| [2](#caso-2--tag--despliegue-automático) | Tag `vX.Y.Z` | Despliegue Continuo automático |
| [3](#caso-3--manual--control-total-y-reversión) | Run workflow | Control manual y reversión |
| [4](#caso-4--ver-la-imagen-versionada-en-el-acr) | ACR | Dónde queda la imagen versionada |
| [5](#caso-5--probar-la-app-con-postman) | Postman | Que la app **de verdad** funciona |
| [6](#caso-6--frenar-y-levantar-la-aplicación) | Frenar / levantar | Acción operativa desde el portal |
| [7](#caso-7--métricas-y-logs-el-matame-la-aplicación) | Métricas | Cómo sé que está viva |
| [8](#caso-8--vulnerabilidades) | Seguridad | Grype y CodeQL |
| [📄](#chuleta-para-tener-a-mano) | Chuleta | Todo en una página |

**Los tres atajos que vas a usar todo el tiempo:**

| Qué | Link |
| :--- | :--- |
| El pipeline | `https://github.com/vaninafuentes/Interview-backend_prats/actions` |
| El portal de Azure | [portal.azure.com](https://portal.azure.com) |
| La app en vivo | `https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io` |

---

# Caso 0 — Preparación (hacelo antes de todo)

> ⚠️ **Sin esto la app no arranca y el health devuelve 503.** Es el error que ya vivimos.

## 0.1 Encender la base de datos

**Dónde:** [portal.azure.com](https://portal.azure.com) → barra de búsqueda de arriba →
escribí **`mysql`** → **Azure Database for MySQL flexible servers** → `mysql-interview-backend`

**Qué tocar:**

1. Fijate arriba el estado. Si dice **`Stopped`**, hay un botón **Start** en la barra de arriba.
2. Hacé clic en **Start** y confirmá.
3. **Esperá 1-2 minutos** hasta que el estado diga **`Ready`**.

**Qué tenés que ver:** el estado pasa de `Stopped` → `Starting` → **`Ready`**.

**Por qué:** la aplicación no arranca sin la base. Se cobra el cómputo solo mientras está encendida, así que después de la demo se apaga.

## 0.2 Verificar que la app responde

**Dónde:** el navegador, una pestaña nueva.

Pegá esta dirección:

```
https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io/actuator/health
```

**Qué tenés que ver:**

```json
{"status":"UP","groups":["liveness","readiness"]}
```

- ✅ **`"status":"UP"`** → está viva, arrancó, llegó a la base. **Seguí.**
- ⚠️ **`503`** → no arrancó. Casi siempre es que la base todavía no está `Ready`.
- ⚠️ **No carga nada / tarda** → está escalada a cero. Refrescá; la primera petición la despierta.

## 0.3 Abrir las dos ventanas que vas a usar

| Ventana | Qué |
| :--- | :--- |
| **GitHub → Actions** | `https://github.com/vaninafuentes/Interview-backend_prats/actions` |
| **Postman** | Con la colección ya importada (ver [Caso 5](#caso-5--probar-la-app-con-postman)) |

---

# Caso 1 — Cambio + push → el pipeline sin desplegar

> **Esto es el "caminito de los pasos" que describió el profesor.** Es el caso más probable
> que te pida en vivo.

## 1.1 Hacer un cambio

**Dónde:** en tu editor, en el repo del backend.

Para la demo conviene un cambio **chico y visible**. Por ejemplo, en
[`AuthController.java`](../src/main/java/com/example/interviewback/controller/AuthController.java):
cambiar el texto de un comentario, o el mensaje de una respuesta.

**Para que se note en el pipeline**, mejor algo que aparezca en el log de arranque. Pero
**cualquier** cambio sirve: lo que se demuestra es el recorrido, no el cambio.

## 1.2 Subirlo

**Dónde:** la terminal, en `C:\Users\vanin\Documents\Interview-backend_prats`.

```bash
git add -A
git commit -m "demo: cambio para mostrar el pipeline en vivo"
git push
```

**Qué tenés que ver:** el push sube el commit sin errores.

## 1.3 Mirar el pipeline

**Dónde:** [GitHub → Actions](https://github.com/vaninafuentes/Interview-backend_prats/actions) → **refrescá la página**.

**Qué tenés que ver:** aparece una ejecución nueva arriba, con el nombre del commit, y un
**círculo amarillo** girando (en curso).

**Hacé clic en ella** para ver los pasos en vivo. Se van a poner en verde de a uno:

| Paso | Qué decir mientras se ejecuta |
| :--- | :--- |
| **Compilar y ejecutar los tests** | *"Los tests no están desactivados. Corren contra un MySQL que el job levanta como contenedor de servicio. Si fallan, el pipeline se corta acá y no llega a publicar."* |
| **Construir la imagen** | *"Las etiquetas las pone el pipeline: el SHA corto del commit siempre, y la versión semántica si es un tag."* |
| **Escanear con Grype** | *"Escanea la imagen terminada: dependencias y sistema operativo. El informe queda como artefacto descargable."* |
| **Login en Azure** | *"Acá entra el Service Principal. No usa mi cuenta: es una identidad aparte, con solo dos permisos."* |
| **Publicar en el ACR** | *"Esto es Integración Continua: ya está integrado y publicado."* |

## 1.4 🔴 El punto clave

**Fijate en el job llamado `deploy`:**

```
deploy — Desplegar en Azure Container Apps    ⊘ skipped
```

**Dice `skipped` (salteado). No se ejecutó.**

**¿Por qué? Y esto es lo que hay que decir:**

> *"Un push a `main` construye y publica, pero **no** despliega. Publicar no es liberar. La
> decisión de liberar es la etiqueta git, que es el Caso 2."*

**Esto es una decisión de diseño, no un error.** Y es exactamente lo que el proyecto de
referencia hacía mal: el script local y el CI podían desplegar cosas distintas.

**Dónde quedó la imagen:** en el ACR, con la etiqueta del SHA corto. Se ve en el [Caso 4](#caso-4--ver-la-imagen-versionada-en-el-acr).

---

# Caso 2 — Tag → despliegue automático

> **Esto es el Despliegue Continuo de verdad:** liberar una versión.

## 2.1 Crear y subir la etiqueta

**Dónde:** la terminal.

```bash
git tag v1.1.1
git push origin v1.1.1
```

> **Si ya usaste `v1.1.1` alguna vez**, cambiá el número (por ejemplo `v1.1.2`).
> Para ver las que ya existen: `git tag`

**Qué tenés que ver:** el push de la etiqueta.

## 2.2 Mirar el pipeline

**Dónde:** GitHub → Actions → refrescá.

**Qué tenés que ver:** **una ejecución nueva**, empezando. La diferencia con el Caso 1 es que
**ahora sí aparece el job `deploy`** y se pone en verde.

**Los pasos del job `deploy`, y qué decir:**

| Paso | Qué dice |
| :--- | :--- |
| **Determinar la versión** | *"De dónde saca qué desplegar: si le indiqué una versión a mano, esa; si no, el tag de git; si no, el SHA del commit."* |
| **Verificar que la imagen existe en el ACR** | *"Antes de tocar nada, confirma que la etiqueta existe. Así no deja la app apuntando a una imagen que no está."* |
| **Desplegar** | *"`az containerapp update`: crea una **revisión nueva** y mueve el tráfico a ella."* |
| **Verificar el health check** | *"**El pipeline no declara éxito hasta que la aplicación responde HTTP 200.** No alcanza con que la imagen se suba."* |

## 2.3 Confirmar en Azure

**Dónde:** [portal.azure.com](https://portal.azure.com) → **Container Apps** → `interview-backend`

**Qué tocar:** en el menú de la izquierda, **Revision management** (o **Revisions**).

**Qué tenés que ver:** una **revisión nueva** arriba, con:
- Nombre terminado en `--v111` (o el tag que usaste)
- Estado **`Active`**
- **`Running`**
- La imagen apuntando a `acrvaniainterview.azurecr.io/interview-backend:v1.1.1`

**Y las revisiones viejas quedan listadas** (hasta 100), que es lo que permite **volver atrás**.

---

# Caso 3 — Manual: control total y reversión

> **El que más impresiona.** Sirve para decidir a mano si desplegar y, sobre todo, para
> **volver a una versión anterior**.

## 3.1 Ejecutar a mano

**Dónde:** [GitHub → Actions](https://github.com/vaninafuentes/Interview-backend_prats/actions)

**Qué tocar, en orden:**

1. En la **lista de la izquierda**, hacé clic en **`CI/CD - Build, Scan, Publicación y Despliegue`**
2. Arriba a la derecha aparece el botón **`Run workflow`** → clic
3. Se abre un panel con:
   - **Use workflow from:** dejá `main`
   - **Desplegar en Azure Container Apps al finalizar:** ☑️ **marcalo** (esto es la clave)
   - **Etiqueta de imagen a desplegar:** dejalo **vacío**
4. Clic en el botón verde **`Run workflow`**

**Qué tenés que ver:** la ejecución aparece arriba y corre los dos jobs, incluido `deploy`.

## 3.2 La reversión (esto es lo que hay que mostrar)

**La gracia:** usando el mismo botón, podés **volver a una versión anterior**.

**Qué tocar:**

1. **`Run workflow`** otra vez
2. **Desplegar:** ☑️ marcado
3. **Etiqueta de imagen a desplegar:** escribí una versión **vieja**, por ejemplo **`v1.1.0`**
4. **`Run workflow`**

**Qué tenés que ver:** en el paso *Determinar la versión* del log dice:

```
Versión a desplegar: v1.1.0 — versión indicada manualmente (redeploy o reversión)
```

Y en Azure, una revisión nueva corriendo la versión anterior.

**Qué decir:**

> *"Las tres formas de disparar el pipeline dan tres resultados distintos y a propósito:
> **push a main** construye y publica sin tocar producción, **el tag** libera automáticamente,
> y **la ejecución manual** me deja elegir si despliego y qué versión. Por eso puedo volver a
> una versión anterior sin reconstruir nada: la imagen ya está en el registro."*

## 3.3 Los tres casos, en una tabla

Esta tabla es **la respuesta a "mostrame el pipeline"**:

| Caso | Qué hacés | Construye | Publica | Despliega |
| :---: | :--- | :---: | :---: | :---: |
| **A** | push a `main` | ✅ | ✅ | ❌ |
| **B** | push del tag `vX.Y.Z` | ✅ | ✅ | ✅ **automático** |
| **C** | Run workflow + casilla | ✅ | ✅ | ✅ **a mano** |

**Y un cuarto caso que también conviene saber:**

| Caso | Qué hacés | Resultado |
| :---: | :--- | :--- |
| **D** | push a una **rama** (no `main`) | ❌ **No pasa nada.** El workflow solo escucha `main` y tags |

---

# Caso 4 — Ver la imagen versionada en el ACR

> **Respuesta a: "¿dónde va a parar la imagen que subo versionada?"**

## 4.1 Desde el portal

**Dónde:** [portal.azure.com](https://portal.azure.com) → barra de búsqueda → **`container registry`** → **Container registries** → **`acrvaniainterview`**

**Qué tocar:** menú de la izquierda → **Repositories** → clic en **`interview-backend`**

**Qué tenés que ver:** la lista de **todas las etiquetas**, cada una con su fecha:

| Etiqueta | Cuándo se creó | Para qué |
| :--- | :--- | :--- |
| `6aec073` (SHA corto) | En cada push | Identifica el commit exacto |
| `v1.1.0`, `v1.1.1` | Solo en los tags | Marca la versión liberada |
| `latest` | En cada push | Comodidad. **El despliegue nunca la usa** |

**Qué decir:**

> *"Cada imagen lleva dos etiquetas: el SHA corto del commit, que identifica exactamente el
> código que la produjo, y la versión semántica si corresponde a un tag. El despliegue siempre
> usa una etiqueta explícita, nunca `latest`, porque `latest` es móvil: si la uso, no sé qué
> código está corriendo y no puedo volver atrás."*

## 4.2 Desde la terminal (si Cloud Shell funciona)

```bash
az acr repository show-tags --name acrvaniainterview --repository interview-backend -o tsv
```

---

# Caso 5 — Probar la app con Postman

> **Respuesta a: "el profesor necesita probar la aplicación, ¿cómo se autentica?"**

## 5.1 Importar la colección

**Dónde:** Postman.

**Qué tocar:**

1. Arriba a la izquierda, **`Import`**
2. Arrastrá el archivo `postman/Interview-Backend.postman_collection.json` del repo
3. **`Import`**

**Qué tenés que ver:** en la izquierda aparece **"Interview AI Backend — Azure"** con 4 carpetas.

## 5.2 Verificar la variable de la URL

**Qué tocar:** clic en el nombre de la colección → pestaña **Variables**.

**Qué tenés que ver:**

| Variable | Valor |
| :--- | :--- |
| `baseUrl` | `https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io` |
| `token` | *(vacío — lo llena solo el script del login)* |
| `email` | el usuario de prueba |
| `password` | la contraseña de prueba |

> ⚠️ **Si la URL cambió**, actualizá `baseUrl` con la que ves en el portal
> (Container Apps → Overview → **Application URL**).

## 5.3 La secuencia para mostrar

**Andá en orden, de arriba hacia abajo:**

| # | Carpeta | Petición | Qué tenés que ver |
| :---: | :--- | :--- | :--- |
| 1 | **0. Salud** | `GET /actuator/health` | **200** y `{"status":"UP"}` |
| 2 | **1. Autenticación** | `POST /api/auth/login` | **200** y en la respuesta viene el **token** |
| 3 | **2. Protegidos** | `GET /api/auth/me` | **200** con tus datos |
| 4 | **2. Protegidos** | `GET /api/questions` | **200** con la lista de preguntas |
| 5 | **2. Protegidos** | `GET /actuator/health` | **200** y ahora con el **detalle de los componentes** |
| 6 | **3. Casos negativos** | `GET /api/auth/me` | **403** ← sin token |

**Los pasos 5 y 6 son los que más impresionan**, así que no los saltees:

**Paso 5 — el health "con token vs sin token":**

> *"Sin token, el health informa solo el estado. Con token, muestra el detalle componente por
> componente, incluida la base de datos. Eso está configurado con `show-details=when-authorized`
> y `roles=USER`: el endpoint tiene que ser público porque lo consultan las sondas de Azure, pero
> no quiero exponer la topología a cualquiera."*

**Paso 6 — el 403:**

> *"Sin token da **403**, no 401. La diferencia importa: 401 es 'no sé quién sos', 403 es 'sé
> quién sos, pero no podés'. Acá el filtro dejó pasar la petición sin autenticar, y fue
> `SecurityConfig` el que la rechazó."*

## 5.4 Por qué el login funciona "de verdad"

> *"El login no es un chequeo falso: busca el usuario en la base de Azure, compara la contraseña
> contra el hash BCrypt guardado, y si coincide firma un JWT con la clave secreta. Que esto
> responda 200 significa que **toda** la cadena funcionó: la variable de entorno de la base, la
> contraseña, el pool de conexiones, la tabla y la clave de firma."*

**Ese es el argumento más fuerte de la demo:** probar el login demuestra que **las variables de
entorno están bien pasadas**. Si faltara alguna, el login fallaría.

---

# Caso 6 — Frenar y levantar la aplicación

> **El profesor dijo que si lo demostrás desde el terminal, tenés punto extra.**

## 6.1 Desde el portal (lo más simple)

**Dónde:** Container Apps → `interview-backend` → **Overview**

**Qué tocar:** buscá el botón **`Stop`** arriba. Si no está, andá a **Application** →
**Scale and replicas** y poné el mínimo en 0.

**Qué tenés que ver:** el estado pasa a **`Stopped`**.

**Y para levantarla:** el botón **`Start`** (o volver el mínimo a su valor).

## 6.2 Desde el terminal (el punto extra)

**Dónde:** [Cloud Shell](https://shell.azure.com) o tu terminal (si Cloud Shell no te funciona).

```bash
# 1. Ver el estado
az containerapp show -n interview-backend -g rg-interview-app \
  --query "properties.runningStatus" -o tsv
```

Debería devolver **`Running`** (o `Scale to 0` si no hay tráfico).

```bash
# 2. DETENER
az rest -m POST -u "/subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-interview-app/providers/Microsoft.App/containerApps/interview-backend/stop?api-version=2025-07-01"
```

```bash
# 3. Verificar que quedó detenida
az containerapp show -n interview-backend -g rg-interview-app \
  --query "properties.runningStatus" -o tsv
```

Debería devolver **`Stopped`**.

```bash
# 4. VOLVER A LEVANTARLA
az rest -m POST -u "/subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-interview-app/providers/Microsoft.App/containerApps/interview-backend/start?api-version=2025-07-01"
```

## 6.3 ⚠️ Dos cosas que NO hay que decir

**Decir cualquiera de estas dos te resta:**

| ❌ Mal | ✅ Bien |
| :--- | :--- |
| *"La detengo con `--max-replicas 0`"* | **Azure no lo permite: el mínimo de `maxReplicas` es 1.** Además escalar a cero no es detener. |
| *"Escalar a cero es lo mismo que detener"* | **No lo es.** Escalar a cero es una configuración de costo: la app **se despierta sola** con la primera petición. Detener es una **acción operativa**: no arranca hasta que la inicies. |

**La frase correcta:**

> *"Se usa la API REST oficial de detener e iniciar. Detener es una acción de nivel de
> aplicación: conserva la configuración, los secretos y la imagen, solo deja de ejecutar
> réplicas. Y no es lo mismo que escalar a cero: escalar a cero es una configuración de costo
> donde la aplicación se despierta sola; detener es una acción operativa donde no arranca hasta
> que la vuelva a iniciar."*

## 6.4 También podés frenar la base

**Y conviene mencionarlo**, porque es la que más gasta:

```bash
az mysql flexible-server stop  -g rg-interview-app -n mysql-interview-backend
az mysql flexible-server start -g rg-interview-app -n mysql-interview-backend
```

---

# Caso 7 — Métricas y logs ("matame la aplicación")

> **El profesor lo planteó así:** *"¿cómo sé que está funcionando? El repo me muestra que salió
> bien y que creó la instancia, pero eso no me dice si está andando."*

**Tiene razón, y esta es la respuesta.**

## 7.1 Las cuatro capas de verificación

| # | Dónde | Qué te dice |
| :---: | :--- | :--- |
| 1 | **El pipeline** | El job `deploy` no declara éxito hasta que `/actuator/health` da 200 |
| 2 | **El health público** | `{"status":"UP"}` o `503` con el motivo |
| 3 | **Azure Monitor** | Réplicas, peticiones, reinicios, CPU, memoria |
| 4 | **Log Analytics** | Los logs de la aplicación, consultables |

## 7.2 Las métricas en el portal

**Dónde:** Container Apps → `interview-backend` → **Metrics** (menú izquierdo)

**Qué tocar:** elegí en el desplegable:

| Métrica | Qué muestra |
| :--- | :--- |
| **Requests** | Cuántas peticiones recibió |
| **Replica Count** | Cuántas réplicas están corriendo (**esto demuestra el escalado a cero**) |
| **Restart Count** | Si se reinició (**si sube, algo anda mal**) |
| **CPU Usage** / **Memory Usage** | Consumo |

**El experimento que vale la pena hacer en vivo:**

1. Abrí **Replica Count** y mirá: probablemente esté en **0** (escalada a cero, no cobra)
2. Desde Postman, mandá el `GET /actuator/health`
3. Volvé a la métrica y refrescá: **pasó a 1**

> *"Eso es el escalado a cero: sin tráfico no hay réplicas y no se factura; con la primera
> petición se crea una. Y las métricas nativas de la JVM también están, porque es un Container
> App: heap, recolector de basura, hilos."*

## 7.3 Los logs

**Dónde:** Container Apps → `interview-backend` → **Log stream** (menú izquierdo)

**Qué tenés que ver:** los logs de arranque de Spring Boot, incluida la línea
`Started InterviewBackApplication in X seconds`.

**Y el historial completo:** Container Apps → **Logs** → consultas KQL. Ahí está el `Connection refused`
del 503, si alguna vez vuelve a pasar.

## 7.4 🎯 La historia que tenés que contar

**Esta es la mejor respuesta posible, porque es real y la viviste:**

> *"El pipeline mostraba verde y la instancia estaba creada, pero la aplicación devolvía 503.
> Ese es exactamente el caso que el profesor describe: **que el despliegue salga bien no
> significa que la aplicación funcione.**
>
> Lo diagnosticamos desde los logs de Log Analytics. Decían `Connection refused`, después
> `JPA EntityManagerFactory`, y al final `Unable to start embedded Tomcat`. La causa era que
> faltaban las variables de entorno de la base de datos. **Con el monitoreo lo encontramos; sin
> el monitoreo habríamos estado adivinando.**"*

## 7.5 ¿Con qué factor de los 12 factores se relaciona?

**Factor XI — Logs: tratá los logs como un flujo de eventos.**

> *"El factor dice que la aplicación nunca debe administrar sus propios archivos de log: emite
> todo por la salida estándar y el entorno decide qué hacer con eso. Nuestra aplicación escribe
> a la consola, Azure captura esa salida y la manda a Log Analytics, donde queda consultable y
> con retención. Eso es el historial."*

**Y las métricas son la otra mitad:** Micrometer las expone y Azure las recolecta. No hay que
instalar ningún agente ni mandar nada a ningún lado.

---

# Caso 8 — Vulnerabilidades

> **El profesor dijo que lo va a chequear con Grype.**

## 8.1 Los resultados

**Estado: 186 → 115 hallazgos (−38 %). Críticas: 10 → 3.**

**Dónde verlo:** GitHub → pestaña **Security** → **Code scanning** (CodeQL) y los artefactos del
pipeline (Grype).

## 8.2 La respuesta a "¿qué hicimos con las vulnerabilidades?"

> *"Usamos dos herramientas que cubren cosas distintas, no una en lugar de la otra:
> **Grype** sobre la imagen terminada —dependencias y sistema operativo— y **CodeQL** sobre
> nuestro código, que es lo que haría Snyk Code.
>
> Bajamos de 186 a 115 hallazgos. Las **3 críticas que quedan están documentadas y
> justificadas**, no ignoradas: `spring-webmvc` 6.2.19 es la última de su rama y la corrección
> está en una versión incompatible con Spring Boot 3.5; y `libc6` depende de que Google
> reconstruya la imagen base, algo que no controlamos.
>
> El mismo hallazgo puede aparecer en las dos herramientas y no es doble conteo: una mira lo que
> escribimos, la otra lo que descargamos."*

## 8.3 La historia del CodeQL, si pregunta

> *"CodeQL encontró 3 alertas de **inyección de registros**: un dato externo llegaba al log sin
> neutralizar, así que alguien podía meter saltos de línea y falsificar entradas.
>
> Primero lo resolví con una expresión regular propia, y CodeQL **volvió a marcar las mismas 3**
> — porque solo reconoce los saneadores que tiene modelados. Lo cambié por **OWASP Java
> Encoder**, que es el estándar de la industria y que CodeQL sí reconoce. Y verifiqué que no
> agregó vulnerabilidades nuevas: 115 antes y 115 después."*

---

# 📄 Chuleta para tener a mano

## Los links

| Qué | Link |
| :--- | :--- |
| El pipeline | `github.com/vaninafuentes/Interview-backend_prats/actions` |
| Los secretos | `github.com/.../settings/secrets/actions` |
| Las alertas | `github.com/.../security/code-scanning` |
| El portal de Azure | [portal.azure.com](https://portal.azure.com) |
| Cloud Shell | [shell.azure.com](https://shell.azure.com) |
| La app | `https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io` |

## Los comandos de git

```bash
git add -A && git commit -m "mensaje" && git push    # Caso A: publica, no despliega
git tag v1.1.2 && git push origin v1.1.2             # Caso B: despliega solo
git tag                                              # ver los tags que ya existen
```

## Dónde está cada cosa en el portal

```
Base de datos     → buscar "mysql" → Flexible servers → mysql-interview-backend
La aplicación     → buscar "container apps" → interview-backend
  ├─ Overview          estado, URL pública, Start/Stop
  ├─ Revisions         qué versión está corriendo
  ├─ Metrics           réplicas, peticiones, reinicios
  ├─ Log stream        logs en vivo
  └─ Logs              historial (KQL)
Las imágenes      → buscar "container registry" → acrvaniainterview → Repositories
Los gastos        → buscar "cost management" → Cost analysis
```

## Las cinco frases que cierran la demo

1. **"Publicar no es liberar."** Un push a `main` construye y publica; solo el tag libera. Eso permite tener una versión lista sin que esté en producción.

2. **"El pipeline no declara éxito hasta que la aplicación responde."** No alcanza con que la imagen se suba: el último paso consulta el health check y espera un 200.

3. **"El código no sabe dónde corre."** Ninguna credencial está en el repositorio; la misma imagen corre igual en mi máquina y en Azure, solo cambian las variables de entorno.

4. **"Que el despliegue salga bien no significa que la aplicación funcione."** Por eso están las métricas y los logs — es el 503 que vivimos.

5. **"El registro no guarda una versión, guarda todas."** Por eso puedo volver a cualquier versión anterior sin reconstruir nada.

## Si algo sale mal

| Síntoma | Causa más probable | Solución |
| :--- | :--- | :--- |
| Health devuelve **503** | La base está apagada | Encenderla ([Caso 0.1](#01-encender-la-base-de-datos)) y esperar `Ready` |
| El health **tarda o no responde** | Está escalada a cero | Refrescar; la primera petición la despierta |
| El job **`deploy` dice `skipped`** | Es lo correcto en un push a `main` | Usar un tag o Run workflow |
| **No aparece ninguna ejecución** | El push fue a una rama, no a `main` | Pushear a `main` |
| El login de Postman da **401** | El usuario de prueba no existe en esa base | Correr primero `POST /api/auth/register` |
| `az` dice **`Please run 'az login'`** | No hay sesión de CLI | Usar Cloud Shell |

---

## Ver también

- [docs/cruce-clase-profesor.md](cruce-clase-profesor.md) — qué dijo el profesor, archivo por archivo, y las respuestas a sus preguntas
- [docs/guia-parcial.md](guia-parcial.md) — las 50 preguntas de defensa
- [docs/ci-cd.md](ci-cd.md) — el detalle del pipeline
- [docs/azure.md](azure.md) — la infraestructura y los costos
