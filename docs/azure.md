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

**Estado de la revisión:** `interview-backend--latest` · activa · Healthy · imagen `v1.0.1`
