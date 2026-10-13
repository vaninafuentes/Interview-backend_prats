# Postman — referencia rápida para el parcial

Todas las peticiones con **todo lo que hay que configurar**: método, URL, body y autenticación.
Copiar y pegar.

---

## 🔗 La URL base

Todo lo que va después de esta dirección:

```
https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io
```

> ⚠️ **Si la app se redespliega, la URL NO cambia.** El FQDN se mantiene.

---

## 👤 Los usuarios de prueba

| Email | Contraseña | Origen |
| :--- | :--- | :--- |
| `prueba@test.com` | `probando123` | Creado para las pruebas |
| `vanina.fuentes@um.edu.ar` | `demo2026unm` | El usuario original |

> 💡 **Si el login da `401`, el usuario no existe en esa base.** Corré el **#2 Register** y después
> volvé al login. Si da `409`, está todo bien: significa que el usuario ya existe.

---

# Las 7 peticiones

## 1️⃣ Health (público)

| | |
| :--- | :--- |
| **Método** | `GET` |
| **URL** | `https://interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io/actuator/health` |
| **Body** | *ninguno* |
| **Auth** | *ninguna* |

**Resultado:** `200`

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

---

## 2️⃣ Register

| | |
| :--- | :--- |
| **Método** | `POST` |
| **URL** | `.../api/auth/register` |
| **Auth** | *ninguna* |
| **Body** | `raw` → **`JSON`** |

```json
{
  "name": "Usuario Prueba",
  "email": "prueba@test.com",
  "password": "probando123"
}
```

**Resultado:** `201` (o `409` si el email ya existe — las dos están bien)

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "id": 2,
  "name": "Usuario Prueba",
  "email": "prueba@test.com"
}
```

---

## 3️⃣ Login

| | |
| :--- | :--- |
| **Método** | `POST` |
| **URL** | `.../api/auth/login` |
| **Auth** | *ninguna* |
| **Body** | `raw` → **`JSON`** |

```json
{
  "email": "prueba@test.com",
  "password": "probando123"
}
```

**Resultado:** `200` ← **de acá sale el token**

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJwcnVlYmFAdGVzdC5jb20i...",
  "id": 2,
  "name": "Usuario Prueba",
  "email": "prueba@test.com"
}
```

> 📋 **Copiá el token**: seleccioná todo el texto entre las comillas de `"token": "..."`.
> Es largo, así que hacé doble clic sobre él y `Ctrl + C`.

---

## 4️⃣ Login con contraseña incorrecta

| | |
| :--- | :--- |
| **Método** | `POST` |
| **URL** | `.../api/auth/login` |
| **Auth** | *ninguna* |
| **Body** | `raw` → **`JSON`** |

```json
{
  "email": "prueba@test.com",
  "password": "estaContrasenaEstaMal"
}
```

**Resultado:** **`401`** ← **tiene que fallar**

```json
{
  "message": "Email o contraseña incorrectos."
}
```

**🗣️ Qué decir:** *"El mensaje es el mismo para 'usuario no existe' y 'contraseña incorrecta', a
propósito: así un atacante no puede averiguar qué emails están registrados."*

---

## 5️⃣ Me CON token

| | |
| :--- | :--- |
| **Método** | `GET` |
| **URL** | `.../api/auth/me` |
| **Body** | *ninguno* |
| **Auth** | pestaña **Auth** → Type: **`Bearer Token`** → Token: **pegá el token** |

> ⚠️ **Pegá SOLO el token.** No escribas `Bearer` — Postman lo agrega solo.
> Si escribís `Authorization Bearer` en la columna *Key* de Headers, te da
> *"headers name must be a valid http token"*.

**Resultado:** `200`

```json
{
  "id": 2,
  "name": "Usuario Prueba",
  "email": "prueba@test.com"
}
```

---

## 6️⃣ Me SIN token

| | |
| :--- | :--- |
| **Método** | `GET` |
| **URL** | `.../api/auth/me` |
| **Body** | *ninguno* |
| **Auth** | pestaña **Auth** → Type: **`No Auth`** |

**Resultado:** **`403`** ← **tiene que fallar**

**🗣️ Qué decir:** *"Da 403, no 401. La diferencia importa: 401 es 'no sé quién sos', 403 es 'sé
quién sos, pero no tenés permiso'. El filtro dejó pasar la petición sin autenticar a propósito, y
fue `SecurityConfig` el que la rechazó — así la autorización queda definida en un solo lugar."*

---

## 7️⃣ Health CON token

| | |
| :--- | :--- |
| **Método** | `GET` |
| **URL** | `.../actuator/health` |
| **Body** | *ninguno* |
| **Auth** | pestaña **Auth** → Type: **`Bearer Token`** → Token: **pegá el token** |

**Resultado:** `200` con **el detalle de los componentes**:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL", ... } },
    "diskSpace": { "status": "UP", ... },
    "ping": { "status": "UP" }
  }
}
```

**🗣️ Qué decir:** *"Sin token informa solo el estado; con token muestra el detalle componente por
componente, incluida la base de datos. Es `show-details=when-authorized` con rol `USER`: el
endpoint tiene que ser público porque lo consultan las sondas de Azure y el pipeline, pero no
quiero exponer la topología de la aplicación a cualquiera."*

---

# 📋 Tabla resumen

| # | Método | Ruta | Auth | Esperado |
| :---: | :---: | :--- | :--- | :---: |
| 1 | `GET` | `/actuator/health` | — | **200** |
| 2 | `POST` | `/api/auth/register` | — | **201** |
| 3 | `POST` | `/api/auth/login` | — | **200** |
| 4 | `POST` | `/api/auth/login` (mal) | — | **401** |
| 5 | `GET` | `/api/auth/me` | Bearer | **200** |
| 6 | `GET` | `/api/auth/me` | No Auth | **403** |
| 7 | `GET` | `/actuator/health` | Bearer | **200** + detalle |

---

# 🎬 El orden para mostrarlos

```
1 → 2 → 3 → 5 → 6 → 4 → 7
```

**Si vas con poco tiempo:** `1 → 2 → 3 → 5 → 6`. Esos cinco ya cuentan toda la historia:
la app vive, me autentico, el token da acceso, sin token no.

---

# 🛟 Si algo falla

| Síntoma | Causa | Solución |
| :--- | :--- | :--- |
| **`401` en el login** | El usuario no existe en esa base | Correr el **#2 Register** primero |
| **`403` en el #5** | El token no se pegó, o venció | Correr el **#3 Login** de nuevo y recopiar |
| **`503`** | La base de Azure está apagada | Encenderla desde el portal |
| **Tarda 20-30 segundos** | Está escalada a cero | **Esperar** — está despertando |
| **`403` en el #1** | La URL está mal escrita | Verificar que termine en `/actuator/health` |
| **Error "headers name must be a valid http token"** | Escribiste `Bearer` en la columna *Key* | Escribí solo `Authorization`, o usá la pestaña **Auth** |

---

## 💡 Tres consejos

1. **Guardá cada petición con `Ctrl + S`.** No dejes ninguna sin guardar — en el parcial no vas a
   querer rearmarlas.

2. **El token caduca a los 7 días.** Si el #5 da `403`, corré el login de nuevo.

3. **Tené dos usuarios de prueba.** Si uno falla, tenés el otro.

---

## Ver también

- [docs/simulacro-parcial.md](simulacro-parcial.md) — la guía completa de la demo
- [postman/Interview-Backend.postman_collection.json](../postman/Interview-Backend.postman_collection.json) — la colección para importar
