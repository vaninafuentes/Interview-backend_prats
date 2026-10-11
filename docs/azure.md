# ARQUITECTURA Y ESTADO REAL DE INFRAESTRUCTURA EN MICROSOFT AZURE

**Proyecto:** Interview AI Backend (Spring Boot)  
**Suscripción:** Azure for Students  
**ID de Suscripción:** `fd36e283-067a-4b69-96e7-27cb9d3cfa2e`  
**Tenant ID:** `47e53f14-2ebb-4bcb-bec0-0bc0ce3d3bbe`  
**Cuenta Propietaria:** `vaninafuentes703@gmail.com`  
**Región Seleccionada:** `brazilsouth` (Brasil Sur — autorizada por política estudiantil y óptima en latencia)  
**Última verificación:** 11 de Octubre de 2026 (estado verificado contra Azure, no documentado de memoria)

---

## 1. RECURSOS APROVISIONADOS Y OPERATIVOS

| Recurso | Nombre Real | Tipo | Estado | SKU / Nivel |
| :--- | :--- | :--- | :---: | :--- |
| **Resource Group** | `rg-interview-app` | `Microsoft.Resources/resourceGroups` | **Succeeded** | N/A (Gratuito) |
| **Container Registry (ACR)** | `acrvaniainterview` | `Microsoft.ContainerRegistry/registries` | **Succeeded** | Basic (`acrvaniainterview.azurecr.io`) |
| **Container Apps Environment** | `env-interview-app` | `Microsoft.App/managedEnvironments` | **Succeeded** | Consumption |
| **Log Analytics Workspace** | `workspace-rginterviewappMrgY` | `Microsoft.OperationalInsights/workspaces` | **Succeeded** | Free tier (ingestión de logs) |
| **Container App** | `interview-backend` | `Microsoft.App/containerApps` | **Succeeded / Healthy** | 0,5 vCPU · 1 GiB · 0-1 réplicas |
| **Base de Datos MySQL** | `mysql-interview-backend` | `Microsoft.DBforMySQL/flexibleServers` | **Ready** | Burstable B1ms · 20 GB · MySQL 8.0 |
| **Service Principal** | `sp-interview-backend` | `Microsoft.EntraID` | Activo | Contributor sobre `rg-interview-app` |

**URL pública del backend:**
`https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io`

---

## 2. RELACIÓN Y JERARQUÍA IMPLEMENTADA

```
[Azure Subscription: Azure for Students]
        │
        ▼
[Resource Group: rg-interview-app] (Región: brazilsouth)
   │
   ├── [Azure Container Registry: acrvaniainterview]
   │        └── acrvaniainterview.azurecr.io/interview-backend:v1.0.1
   │
   ├── [MySQL Flexible Server: mysql-interview-backend]
   │        ├── mysql-interview-backend.mysql.database.azure.com:3306
   │        ├── Base de datos: interviewai_db (utf8mb4)
   │        └── Regla de firewall: AllowAzureServices (0.0.0.0 / 0.0.0.0)
   │
   └── [Container Apps Environment: env-interview-app]
             ├── [Log Analytics: workspace-rginterviewappMrgY]
             └── [Container App: interview-backend]
                       ├── Imagen: interview-backend:v1.0.1
                       ├── Puerto: 8080 (ingress externo, HTTPS)
                       └── Spring Boot + Hibernate + HikariCP
```

**Flujo de una petición:**

```
App React Native / Postman
        │ HTTPS
        ▼
Container App (ingress externo, puerto 8080)
        │
        ▼
Spring Boot  →  JwtAuthFilter  →  Controllers
        │
        ▼
HikariCP (pool InterviewHikariPool, máx. 10 conexiones)
        │ JDBC + SSL (sslMode=REQUIRED)
        ▼
MySQL Flexible Server → interviewai_db
```

---

## 3. CONFIGURACIÓN DEL CONTAINER APP (nombres, sin valores)

**Imagen desplegada:** `acrvaniainterview.azurecr.io/interview-backend:v1.0.1`
*(se despliega siempre por etiqueta explícita, nunca por `latest`)*

**Variables de entorno configuradas:**

| Variable | Origen del valor |
| :--- | :--- |
| `PORT` | `8080` |
| `SPRING_DATASOURCE_URL` | FQDN de Azure MySQL + `sslMode=REQUIRED` |
| `SPRING_DATASOURCE_USERNAME` | `interviewadmin` |
| `SPRING_DATASOURCE_PASSWORD` | 🔒 `secretref:db-password` |
| `JWT_SECRET` | 🔒 `secretref:jwt-secret` |
| `JWT_EXPIRATION_MS` | `604800000` (7 días) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| `SPRING_JPA_SHOW_SQL` | `false` |

**Secrets del Container App:** `db-password`, `jwt-secret` (más el secret de acceso al registro, generado automáticamente por Azure).

> ⚠️ Los valores nunca se documentan ni se commitean. Viven en Azure Container Apps Secrets y, en el entorno local,
> en `.secrets/` (ignorado por git).

---

## 4. POLÍTICA DE SUSCRIPCIÓN ESTUDIANTIL DETECTADA

La política activa `sys.regionrestriction` restringe el despliegue a estas regiones:
`brazilsouth`, `northcentralus`, `mexicocentral`, `canadacentral`, `southafricanorth`.

**Todos los recursos quedaron alineados en `brazilsouth`.**

Además fue necesario **registrar el resource provider** `Microsoft.DBforMySQL` (una sola vez por suscripción,
gratuito):

```bash
az provider register --namespace Microsoft.DBforMySQL --wait
```

---

## 5. COSTOS Y CONTROL DE GASTOS

Crédito disponible: **USD 100** (Azure for Students).

| Recurso | Costo | Notas |
| :--- | :--- | :--- |
| Resource Group, ACR Basic, Container Apps Environment, Log Analytics | ≈ USD 5/mes | ACR Basic ≈ USD 5/mes fijo |
| Container App | USD 0 en reposo | **`minReplicas = 0`**: escala a cero sin tráfico |
| MySQL Flexible Server (B1ms) | USD 0,035/hora | ≈ USD 25,55/mes si corre 24/7 |
| Almacenamiento MySQL (20 GB) | USD 0,141/GB/mes | ≈ USD 2,83/mes siempre |
| Backup MySQL | Incluido | 100 % del tamaño del storage, retención 7 días |

**Reglas de ahorro aplicadas:**
- `storage-auto-grow: Disabled` → el disco **no crece solo** (evita cargos sorpresa).
- Alta disponibilidad `Disabled` → no duplica el costo.
- `minReplicas: 0` en el Container App → el backend no consume cuando nadie lo usa.
- La base se apaga entre sesiones de trabajo (ver comandos abajo): detenida **solo se cobra el storage**.

**Costo real esperado:** ≈ USD 8-10/mes con disciplina de encendido (`start` antes de trabajar, `stop` al terminar).

### Comandos de administración de costos

```bash
# Detener la base (deja de cobrar cómputo; se cobra solo el storage)
az mysql flexible-server stop -g rg-interview-app -n mysql-interview-backend

# Encender la base
az mysql flexible-server start -g rg-interview-app -n mysql-interview-backend

# Consultar el estado
az mysql flexible-server list -o table
```

> ⚠️ Azure reinicia automáticamente un servidor detenido después de 30 días.

### Qué pasa cuando la base está detenida

| Aspecto | Comportamiento |
| :--- | :--- |
| **Costo** | Se deja de cobrar el cómputo (USD 0,035/h). **Sí se sigue cobrando el storage**: ≈ USD 2,83/mes. |
| **Datos** | Se conservan intactos. Detener no borra nada. |
| **Backend en Azure** | Sigue desplegado, pero **falla** si recibe una petición: Hibernate no puede conectar y la aplicación no arranca (el mismo error que causaba el 503 original). `/actuator/health` reportaría `db: DOWN`. |
| **IP / FQDN** | Se conservan. |

### 🔴 ORDEN OBLIGATORIO ANTES DE UNA DEMO O DEL PARCIAL

```bash
# 1. Encender la base y esperar a que quede lista (tarda ~1-2 minutos)
az mysql flexible-server start -g rg-interview-app -n mysql-interview-backend

# 2. Verificar que quedó Ready ANTES de probar la aplicación
az mysql flexible-server list -o table          # debe decir "Ready"

# 3. Recién ahora probar el backend
curl https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io/actuator/health
```

Si se prueba el backend con la base detenida, Azure registra una réplica fallida; conviene forzar una
nueva revisión o simplemente reintentar la petición una vez que la base esté `Ready`.

### Eliminación total al finalizar el cuatrimestre

```bash
# Borra TODO (ACR, Container App, MySQL, Log Analytics) en una sola operación.
# Irreversible: pedir confirmación antes de ejecutar.
az group delete --name rg-interview-app --yes --no-wait
```

---

## 6. PROBLEMAS CONOCIDOS DEL CLI DE AZURE (documentados el 11/10/2026)

| Síntoma | Causa | Solución aplicada |
| :--- | :--- | :--- |
| `az mysql flexible-server create` devuelve `InternalServerError`… | El CLI falla al interpretar la respuesta asíncrona, **pero el recurso se crea igual** | Verificar siempre con `az mysql flexible-server list`. Si el servidor existe, ignorar el error. |
| `az mysql flexible-server firewall-rule create` devuelve `InternalServerError` siempre | Bug del CLI en esta operación | Crear la regla por la API de ARM con `az rest` |
| `az mysql flexible-server execute` no existe | El subcomando fue removido/movido en esta versión | Usar un cliente MySQL (por ejemplo `docker run --rm mysql:8 mysql -h …`) |
| `az containerapp logs show` falla con `'eventStreamEndpoint'` | La app está en 0 réplicas: no hay contenedor vivo del cual transmitir logs | Consultar Log Analytics (ver `docs/monitoreo.md`) o esperar a que haya una réplica activa |

### Crear una regla de firewall (para conectarse desde una IP local)

```bash
# Reemplazar <TU_IP_PUBLICA> (obtenerla con: curl -s https://api.ipify.org)
az rest --method put --url "https://management.azure.com/subscriptions/<SUBSCRIPTION_ID>/resourceGroups/rg-interview-app/providers/Microsoft.DBforMySQL/flexibleServers/mysql-interview-backend/firewallRules/MiIP?api-version=2023-12-30" \
  --body '{"properties":{"startIpAddress":"<TU_IP_PUBLICA>","endIpAddress":"<TU_IP_PUBLICA>"}}'
```

> Por seguridad, **la regla temporal usada durante la configuración fue eliminada**. Solo queda
> `AllowAzureServices`, que es la que necesita Container Apps. Recrearla solo si hace falta conectarse
> con un cliente MySQL local, y borrarla al terminar.

---

## 7. VERIFICACIÓN DEL DESPLIEGUE (11/10/2026)

Pruebas ejecutadas contra la URL pública real:

| Prueba | Resultado |
| :--- | :--- |
| `GET /actuator/health` | **200** — `status: UP`, `db: {"status":"UP","database":"MySQL"}` |
| `POST /api/auth/register` | **201** + token JWT |
| `POST /api/auth/login` | **200** + token JWT |
| `GET /api/auth/me` sin token | **403** |
| `GET /api/auth/me` con token | **200** + datos del usuario |
| `GET /api/auth/me` con token adulterado | **403** |
| `POST /api/auth/login` con password incorrecta | **401** |
| `GET /api/questions` con token | **200** |
| Tablas creadas por Hibernate en Azure MySQL | `users`, `questions`, `interview_sessions` |
| Datos sembrados (DataSeeder) | 10 preguntas |

**Estado de la revisión:** `interview-backend--latest` · activa · Healthy

---

## 8. ADMINISTRACIÓN DE AZURE CONTAINER APPS (runbook)

> **Herramienta:** para las operaciones del día a día existe `scripts/admin.sh`
> (`bash scripts/admin.sh`). Abajo está el detalle de cada operación, con su equivalente
> en el Portal y en la terminal, sus consecuencias y sus riesgos.

### ⚠️ Distinción fundamental

| | Backend | Base de datos |
| :--- | :--- | :--- |
| **Servicio** | Azure Container Apps | Azure Database for MySQL |
| **Recurso** | `interview-backend` | `mysql-interview-backend` |
| **Sección del portal** | *Container Apps* | *Azure Database for MySQL flexible servers* |

**No confundir:** apagar desde *Container Apps → Stop* **apaga el backend, no la base**. Y hacerlo puede
dejar el entorno marcado para auto-borrado (`ScheduledForDelete`), lo que rompe el próximo despliegue.

### Tabla de operaciones

| Operación | Desde el Portal | Desde la terminal | Cómo verificar | Consecuencias |
| :--- | :--- | :--- | :--- | :--- |
| **Ver el estado** | *Container Apps* → `interview-backend` → **Overview** | `az containerapp show -n interview-backend -g rg-interview-app` | `provisioningState = Succeeded` | Ninguna (solo lectura) |
| **Ver la imagen desplegada** | **Application → Revisions** | `az containerapp show … --query "properties.template.containers[0].image"` | Devuelve `…:9104691` | Ninguna |
| **Obtener la URL** | **Overview → Application Url** | `az containerapp show … --query "properties.configuration.ingress.fqdn" -o tsv` | `curl https://<fqdn>/actuator/health` | Ninguna |
| **Ver revisiones** | **Application → Revisions and replicas** | `az containerapp revision list -n interview-backend -g rg-interview-app -o table` | Aparece una revisión *Active* con tráfico 100 % | Ninguna |
| **Desplegar una versión** | **Application → Revisions → Create new revision** | `az containerapp update -n … -g … --image acrvaniainterview.azurecr.io/interview-backend:v1.0.1` | La revisión nueva queda *Active* y el health devuelve 200 | Crea una revisión nueva. **Momentáneo de indisponibilidad** durante el arranque |
| **Revertir (rollback)** | Igual que desplegar, eligiendo una **revisión anterior** | Igual que desplegar, con una etiqueta anterior del ACR | La imagen desplegada es la versión buscada | Igual que desplegar. **No se pierde ningún dato** |
| **Ver logs en vivo** | **Monitoring → Log stream** | `az containerapp logs show -n … -g … --tail 50` | Se ven las líneas de Spring Boot | Ninguna. ⚠️ Falla si la app está en 0 réplicas |
| **Ver logs históricos** | **Monitoring → Logs** (Log Analytics) | Consulta a Log Analytics (ver `docs/monitoreo.md`) | Aparecen eventos de arranques anteriores | Ninguna |
| **Start de la APP** | *Container Apps* → **Start** | `az containerapp start -n … -g …` | Estado `Running` | **No usar** salvo necesidad: ya escala sola desde cero |
| **Stop de la APP** | *Container Apps* → **Stop** | `az containerapp stop -n … -g …` | Estado `Stopped` | ⚠️ **Riesgo de que el entorno quede marcado para auto-borrado.** Preferir dejarla en 0 réplicas |
| **Nueva revisión (restart)** | **Application → Revisions → Create new revision** | `az containerapp revision restart -n … -g … --revision <rev>` | La revisión reinicia el contenedor | Corta el servicio unos segundos. Útil cuando la app arrancó mal |
| **Encender la base** | *MySQL flexible servers* → `mysql-interview-backend` → **Start** | `az mysql flexible-server start -g rg-interview-app -n mysql-interview-backend` | `state = Ready` (tarda 1-2 min) | Empieza a cobrarse el cómputo |
| **Detener la base** | Igual → **Stop** | `az mysql flexible-server stop -g rg-interview-app -n mysql-interview-backend` | `state = Stopped` | Deja de cobrarse el cómputo (sigue el storage). **No se pierden datos** |
| **Eliminar la APP** | *Container Apps* → `interview-backend` → **Delete** | `az containerapp delete -n … -g … --yes` | Desaparece de la lista | ❌ **Irreversible.** Se pierden la configuración y los secretos. La imagen del ACR, la base y los logs **no** se pierden |
| **Eliminar TODO** | *Resource groups* → `rg-interview-app` → **Delete resource group** | `az group delete --name rg-interview-app --yes` | El grupo desaparece | ❌ **Irreversible.** Borra ACR, app, entorno, MySQL y logs. Es la forma correcta de terminar el cuatrimestre |

### Reglas de seguridad para operar

1. **Nunca** eliminar recursos sin autorización explícita.
2. **Antes de una demo:** encender la base → esperar `Ready` → recién ahí probar la aplicación.
3. **El despliegue se hace desde el pipeline** (pestaña Actions de GitHub), no a mano. Si se despliega
   manualmente, usar **siempre una etiqueta publicada por el CI**, nunca `latest`.
4. Si un push no debe desplegarse, simplemente **no se marca la casilla** de deploy.

### Sobre `script_az.py` del proyecto de referencia

El proyecto de Go tenía un script de ~1400 líneas que construía, publicaba y desplegaba manualmente.
**Ese script no se adaptó a propósito**, y la razón es el problema que ese mismo proyecto arrastraba:

- El script local y el CI construían imágenes con **criterios de etiquetado distintos**, por lo que un
  despliegue manual podía terminar usando una imagen diferente de la que había generado el pipeline.

En este proyecto la responsabilidad está separada de forma estricta:

| Tarea | Quién la hace |
| :--- | :--- |
| Construir la imagen, escanearla y publicarla | **GitHub Actions** (único lugar) |
| Desplegar | **GitHub Actions** (job condicionado) |
| Operar (logs, estado, encender/apagar la base, revertir, eliminar) | `scripts/admin.sh` |

Así, **todo lo que corre en Azure pasó obligatoriamente por los tests y por Grype**.
