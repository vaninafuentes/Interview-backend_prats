# GUÍA DE CONCEPTOS — TODO EXPLICADO DESDE CERO

**Para qué sirve este documento:** explica **cada concepto** que usamos en el proyecto, suponiendo que
no sabés nada del tema. No es la documentación técnica (esa está en los otros archivos): acá está el
**porqué de las cosas**, con analogías y en el orden en que conviene estudiarlas.

**Cómo leerlo:** cada concepto tiene tres partes:
1. **Qué problema resuelve** (por qué existe)
2. **Qué es** (la definición)
3. **Dónde lo usamos** (en nuestro proyecto)

---

# ÍNDICE

| Parte | Tema |
| :--- | :--- |
| **1** | La aplicación: API, Spring Boot, base de datos |
| **2** | Seguridad: autenticación, JWT, contraseñas |
| **3** | Contenedores: Docker, imágenes, Compose |
| **4** | La nube: Azure, suscripción, grupos, Service Principal |
| **5** | CI/CD: GitHub Actions, integración y despliegue continuo |
| **6** | Operación: observabilidad, monitoreo, análisis de seguridad |
| **7** | El recorrido completo: de tu teclado a Azure |
| **8** | Glosario |

---
---

# PARTE 1 — LA APLICACIÓN

## 1.1 ¿Qué es una API REST?

**El problema que resuelve:** una aplicación móvil necesita datos que están en un servidor. ¿Cómo se piden?

**Qué es:** una **API** (*Application Programming Interface*) es un conjunto de "puertas de entrada" a un
sistema. **REST** es un estilo para diseñar esas puertas usando el protocolo **HTTP** (el mismo de las
páginas web).

Cada puerta se identifica con una **URL** y un **método HTTP**:

| Método | Para qué | Ejemplo nuestro |
| :--- | :--- | :--- |
| `GET` | **Leer** algo | `GET /api/questions` — dame las preguntas |
| `POST` | **Crear** algo o enviar datos | `POST /api/auth/login` — acá van mis credenciales |
| `PUT` | **Reemplazar** algo | *(no lo usamos)* |
| `DELETE` | **Borrar** algo | *(no lo usamos)* |

**Analogía:** un restaurante. La URL es el plato que pedís, el método HTTP es si pedís, entregás o cancelás, y
el **JSON** es el idioma en que te responden.

**Códigos de respuesta HTTP** — son el "cómo salió":

| Código | Significado | Cuándo lo devolvemos |
| :--- | :--- | :--- |
| **200** OK | Todo bien | Login exitoso, listar preguntas |
| **201** Created | Se creó algo | Registro de usuario |
| **400** Bad Request | Mandaste mal los datos | Contraseña de menos de 6 caracteres |
| **401** Unauthorized | **No sé quién sos** | Login con contraseña incorrecta |
| **403** Forbidden | **Sé quién sos, pero no podés** | Pedís algo sin token, o con token inválido |
| **404** Not Found | No existe | Pregunta con un id inexistente |
| **409** Conflict | Choca con algo existente | Te registrás con un email ya usado |
| **500** Server Error | **Se rompió el servidor** | Un error no previsto |

> 🔑 **Diferencia clave 401 vs 403:** 401 es *"no me probaste quién sos"*; 403 es *"me lo probaste, pero no
> tenés permiso"*. En nuestro proyecto: contraseña incorrecta → **401**; sin token o token inválido → **403**
> (porque Spring Security no considera que estés autenticado y directamente bloquea el acceso).

---

## 1.2 ¿Qué es Spring Boot?

**El problema que resuelve:** hacer una aplicación Java web "a mano" es un infierno: configurar el servidor,
conectar todo, manejar el ciclo de vida de los objetos…

**Qué es:** un **framework** de Java que trae todo eso resuelto. Tres ideas centrales:

### a) Servidor web incluido (*embedded*)

Normalmente una aplicación Java necesita un servidor externo (Tomcat) donde "desplegarse". Spring Boot
**trae Tomcat adentro** de la aplicación: es un programa que se ejecuta y ya queda escuchando en un puerto.
Eso es lo que permite empaquetarlo en un **JAR ejecutable** (`java -jar app.jar`).

### b) Inversión de control y **inyección de dependencias** ⭐

Este es el concepto **más importante** de Spring. Prestá atención:

**Sin Spring (lo que NO se hace):**
```java
public class AuthController {
    public AuthResponse login(LoginRequest r) {
        UserRepository repo = new UserRepository();        // yo lo creo
        PasswordEncoder enc = new BCryptPasswordEncoder(); // yo lo creo
        AuthService service = new AuthService(repo, enc);  // yo lo armo
        return service.login(r);
    }
}
```

**Problemas:** cada vez que llega una petición se crean objetos nuevos (costoso); si `AuthService` cambia su
constructor, hay que tocar todos lados; y no se puede reemplazar `UserRepository` por una versión de prueba.

**Con Spring (lo que sí se hace):**
```java
@Service
public class AuthService {
    private final UserRepository userRepository;         // lo declaro
    private final PasswordEncoder passwordEncoder;       // lo declaro

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;            // Spring me lo entrega
        this.passwordEncoder = passwordEncoder;
    }
}
```

Yo **declaro qué necesito** y **Spring me lo entrega**. Yo no creo nada.

**Cómo funciona:** al arrancar, Spring **escanea** los paquetes buscando clases anotadas, crea **una instancia
de cada una**, y las guarda en un registro. Ese registro es el **contexto de aplicación** (`ApplicationContext`)
— podés imaginarlo como una **caja de herramientas** que Spring llena al arrancar. Cuando algo necesita una
herramienta, Spring se la pasa. Las instancias que viven en esa caja se llaman **beans**.

**Las anotaciones que hay que saber:**

| Anotación | Qué le dice a Spring | Dónde la usamos |
| :--- | :--- | :--- |
| `@SpringBootApplication` | "Arrancá desde acá y escaneá todo este paquete" | `InterviewBackApplication` |
| `@RestController` | "Esta clase atiende peticiones HTTP y devuelve JSON" | Los 3 controllers |
| `@Service` | "Esta clase tiene lógica de negocio" | Los 4 services |
| `@Repository` | "Esta clase accede a la base de datos" | Los 3 repositorios |
| `@Component` | "Esta clase es un componente genérico" | `JwtUtil`, `JwtAuthFilter` |
| `@Configuration` | "Esta clase define beans" | `SecurityConfig`, `CorsConfig` |
| `@Bean` | "El objeto que devuelve este método es un bean" | `PasswordEncoder` |
| `@Autowired` | "Inyectame esto" | **No lo usamos** (ver abajo) |
| `@Value` | "Leé este valor del archivo de configuración" | `JwtUtil` |
| `@Override` | "Estoy reemplazando un método heredado" | Muchos |

**`@RestController`, `@Service`, `@Repository` y `@Component` son lo mismo por dentro:** todos le dicen a
Spring "creá una instancia de esta clase". Los usamos distintos para **expresar la intención** (que se lee
mejor), pero técnicamente son equivalentes.

> **¿Por qué NO escribimos `@Autowired`?**
> Cuando una clase tiene **un solo constructor**, Spring lo usa automáticamente. Escribirlo sería redundante.
> Además se prefiere la **inyección por constructor** sobre `@Autowired` en el campo, porque las dependencias
> quedan `final` (no cambian nunca), se ven en la firma del constructor, y la clase se puede instanciar a
> mano en una prueba sin levantar todo Spring.

**Singleton:** por defecto Spring crea **una sola instancia** de cada bean, que se comparte en toda la
aplicación. Por eso `PasswordEncoder` es el mismo objeto para `AuthService` (login) y para `SecurityConfig`:
si cada uno creara el suyo, podrían estar configurados distinto y el login fallaría.

---

## 1.3 ¿Qué es una base de datos relacional y MySQL?

**El problema:** si el servidor se reinicia, ¿se pierden los usuarios? Una base de datos guarda los datos de
forma **permanente** y permite consultarlos rápido.

**Qué es:** una base **relacional** organiza los datos en **tablas** (como planillas de Excel) con **filas**
(cada registro) y **columnas** (cada dato). **MySQL** es el motor más usado del mundo para esto.

**Nuestras tablas:**

| Tabla | Qué guarda |
| :--- | :--- |
| `users` | Los usuarios: id, nombre, email, contraseña (hasheada) |
| `questions` | Las preguntas de práctica: id, texto, categoría, dificultad |
| `interview_sessions` | Cada respuesta que dio un usuario y la evaluación que recibió |

**SQL** es el idioma para hablarle a la base:
```sql
SELECT * FROM users WHERE email = 'juan@mail.com';
```

---

## 1.4 JPA, Hibernate y Spring Data JPA — los tres juntos

Acá hay mucha confusión. Son **tres cosas distintas** que trabajan en capas:

```
Tu código Java  →  Spring Data JPA  →  JPA (interfaz)  →  Hibernate  →  MySQL
                   (los repositorios)   (la norma)          (el que hace el trabajo)
```

| | Qué es | Analogía |
| :--- | :--- | :--- |
| **JPA** | Una **especificación**: el conjunto de reglas que dicen cómo mapear objetos Java a tablas | La **norma** que dice cómo debe ser un enchufe |
| **Hibernate** | La **implementación** concreta de esa norma, la que realmente hace el trabajo | El **fabricante** que hace el enchufe |
| **Spring Data JPA** | Una **capa de conveniencia** sobre Hibernate: le declarás la interfaz y él escribe la implementación | El **adaptador** que te evita soldar cables |

**Por qué esto importa:** en nuestro proyecto **no escribimos ni una línea de SQL**. Solo declaramos:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

Spring lee el nombre `findByEmail` y **genera la consulta solo**: `SELECT * FROM users WHERE email = ?`.

**Los "3 conceptos mágicos" que hay que entender:**

1. **Entidad:** una clase Java que representa una tabla. Se marca con `@Entity`. Cada objeto es una fila.
   ```java
   @Entity
   @Table(name = "users")
   public class User {
       @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       @Column(nullable = false, unique = true)
       private String email;
   }
   ```
   - `@Id` → cuál es la clave primaria (el identificador único)
   - `@GeneratedValue(IDENTITY)` → el id lo genera la base sola (el `AUTO_INCREMENT` de MySQL)
   - `@Column` → cómo se llama y se comporta la columna

2. **Repositorio:** la interfaz que da acceso a los datos. Hereda `JpaRepository` y ya tiene `save()`,
   `findById()`, `findAll()`, `deleteById()` sin escribir nada.

3. **Transacción:** un grupo de operaciones que se ejecutan **todas o ninguna**. Es la garantía de que la
   base no quede a medias.

---

## 1.5 Transacciones y `@Transactional`

**El problema:** en el registro, hacemos dos cosas: (1) verificar si el email ya existe, (2) insertar el
usuario. ¿Qué pasa si entre el paso 1 y el 2 **otro usuario se registra con el mismo email al mismo tiempo**?
Los dos pasaron la verificación, y terminamos con dos usuarios con el mismo email.

**Qué es una transacción:** una operación **atómica** — o se hace completa, o no se hace nada. Si algo falla
en el medio, se **revierte** todo (`ROLLBACK`). **Analogía:** una transferencia bancaria: si sale la plata de
tu cuenta pero no entra en la otra, la operación se deshace.

**En Spring:** se marca el método con `@Transactional`.

```java
@Transactional                              // ESCRIBE: todo o nada
public AuthResponse register(RegisterRequest request) { ... }

@Transactional(readOnly = true)             // SOLO LEE
public List<Question> getAllQuestions() { ... }
```

**¿Qué hace `readOnly = true`?** Es una **optimización**, no una protección:
- Le dice a Hibernate que no hace falta el *dirty checking* (comparar cada entidad cargada con su estado
  original al terminar, para detectar cambios)
- Permite que el driver derive la lectura a una **réplica** de la base si existe

> ⚠️ **ERROR FRECUENTE:** `readOnly = true` **NO evita la inyección SQL**. Quien la evita son las
> **consultas parametrizadas**: en vez de armar el texto de la consulta pegando el valor
> (`"WHERE email = '" + email + "'"`), se envía el valor **como parámetro aparte** al `PreparedStatement`
> del driver. Así, si alguien manda `' OR 1=1 --`, el driver lo trata como **texto literal** y no como
> código SQL. Spring Data JPA hace esto automáticamente.

**Decisión nuestra:** en `evaluate` y `getHint` **no** ponemos `@Transactional` **a propósito**. Esos métodos
llaman a la API de IA (lenta y externa). Si la transacción estuviera abierta durante esa llamada, retendría
una conexión del pool sin usarla — y con un pool de 10 conexiones, alcanzaría para agotarlo.

---

## 1.6 HikariCP: el pool de conexiones

**El problema:** conectarse a la base es **caro** — hay que abrir una conexión TCP, autenticarse, negociar
TLS. Si cada petición hiciera eso, la aplicación sería lentísima.

**Qué es:** un **pool** (pileta) de conexiones. Al arrancar, abre varias conexiones y las **mantiene
abiertas**. Cuando una petición necesita la base, **pide prestada** una y la **devuelve** al terminar.

**Analogía:** en vez de contratar y despedir un empleado por cada tarea, tenés un equipo fijo que va rotando.

**Nuestra configuración:**

| Parámetro | Valor | Para qué |
| :--- | ---: | :--- |
| `maximum-pool-size` | 10 | Máximo de conexiones abiertas a la vez |
| `minimum-idle` | 2 | Cuántas mantener listas aunque no se usen |
| `connection-timeout` | 20 s | Cuánto esperar una conexión libre antes de fallar |
| `idle-timeout` | 5 min | Cerrar conexiones ociosas |
| `max-lifetime` | 20 min | Renovar conexiones viejas (evita conexiones "muertas") |
| `leak-detection-threshold` | 30 s | Avisar en el log si alguien no devuelve la conexión |

> ⭐ **Pregunta típica del parcial: "¿qué pasa con el pool si escalás la aplicación?"**
> El pool es **por instancia**, no global. Con el pool en 10 y **1 réplica**, el máximo de conexiones a MySQL
> es 10. Si el Container App escalara a **3 réplicas**, serían **30 conexiones simultáneas**. Por eso el
> tamaño del pool se elige pensando en el peor caso de escalado y en el límite de conexiones del servidor.

---
---

# PARTE 2 — SEGURIDAD

## 2.1 Autenticación vs autorización

Son dos cosas distintas que suenan parecido:

| | Pregunta que responde | Quién la resuelve en nuestro proyecto |
| :--- | :--- | :--- |
| **Autenticación** | *¿Quién sos?* | `JwtAuthFilter` (valida el token) |
| **Autorización** | *¿Podés hacer esto?* | `SecurityConfig` (las reglas de acceso) |

**Analogía:** en un edificio, la autenticación es mostrar el DNI en la entrada; la autorización es si tu
credencial te abre la puerta del piso 5.

**Decisión de diseño nuestra:** están **separadas a propósito**. El filtro **no rechaza** nada, solo
autentica (dice "este es Juan"). El rechazo lo decide `SecurityConfig` en un único lugar. Si el filtro
rechazara, las reglas de acceso estarían repartidas en dos lugares y sería fácil que se contradigan.

---

## 2.2 ¿Qué es un token y qué es JWT?

**El problema que resuelve:** HTTP **no tiene memoria**. Cada petición es independiente. Si te autenticás en
una, ¿cómo sabe el servidor quién sos en la siguiente?

**Solución tradicional (sesiones):** el servidor guarda en memoria "el usuario X está logueado" y te da una
**cookie** con un número. Problema: si tenés **varias instancias** de la aplicación (escalado horizontal),
cada una tiene su propia memoria y no sabe de las sesiones de las otras. Habría que compartir esa memoria.

**Solución con JWT:** el servidor **no guarda nada**. En vez de eso, te entrega un **token firmado** que
contiene tus datos. En cada petición lo mandás, y el servidor **verifica la firma**. Si es válida, sos vos.

**Analogía:** la sesión es una **ficha que el guardia guarda en su casillero**. El JWT es una **entrada de
papel con un sello de seguridad**: cualquiera puede mirar el sello y saber que es auténtica, sin consultar
ningún registro.

**Cómo es un JWT por dentro:** tres partes separadas por puntos:

```
eyJhbGciOiJIUzM4NCJ9  .  eyJzdWIiOiJqdWFuQG1haWwuY29tIn0  .  dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
      HEADER                        PAYLOAD                          SIGNATURE
```

| Parte | Contenido | Nuestro caso |
| :--- | :--- | :--- |
| **Header** | Algoritmo y tipo | `{"alg":"HS384","typ":"JWT"}` |
| **Payload** | Los datos (*claims*) | `{"sub":"juan@mail.com", "name":"Juan", "id":3, "iat":..., "exp":...}` |
| **Signature** | La firma | HMAC-SHA384 sobre el header y el payload, con una clave secreta |

**Los claims que usamos:**

| Claim | Significa |
| :--- | :--- |
| `sub` (subject) | El email del usuario |
| `iat` (issued at) | Cuándo se emitió |
| `exp` (expiration) | Cuándo vence (7 días en nuestro caso) |
| `name`, `id` | Datos propios nuestros |

> ⚠️ **CONCEPTO CRÍTICO:** el payload **NO está cifrado, solo codificado en Base64**. Cualquiera puede
> leerlo (pegalo en jwt.io y lo ves). Lo que la firma garantiza es que **nadie pueda modificarlo** sin la
> clave secreta. Por eso **NUNCA** se pone información sensible (como una contraseña) en el payload.

**Cómo se valida un token:** dos verificaciones:
1. **La firma** — se recalcula con la clave secreta y se compara. Si alguien cambió aunque sea un carácter
   del payload, la firma no coincide → se rechaza.
2. **La expiración** — se compara `exp` con la hora actual.

Se hace **sin consultar la base de datos**: es una verificación criptográfica local, rapidísima.

> **Detalle fino:** el algoritmo exacto lo elige la librería (`jjwt`) según el tamaño de la clave. Con
> nuestra clave de 48 bytes usa **HS384**; con una de 32 usaría HS256. Una clave de menos de 32 bytes la
> rechaza, evitando firmas débiles.

**¿Por qué no ponemos los roles adentro del token?** Porque el token dura 7 días: si a un usuario le cambiás
los permisos, tardarían 7 días en aplicarse. Como nuestro filtro **consulta la base en cada petición** para
cargar el usuario, los permisos reflejan siempre el estado actual.

---

## 2.3 Contraseñas: hash, no cifrado

**El problema:** si guardás las contraseñas en texto plano y te hackean la base, todos tus usuarios quedan
expuestos.

**Lo que NO hay que hacer: cifrar.** Cifrar es **reversible**: existe una llave que lo deshace. Si esa llave
está en el sistema, el atacante la consigue.

**Lo que SÍ se hace: hash.** Un hash es una función de **un solo sentido**: convierte `"secreto123"` en algo
como `$2a$10$N9qo8uLO...` y **no hay forma de volver atrás**.

**¿Cómo se verifica entonces la contraseña?** No se deshace el hash: se **hashea lo que el usuario escribió**
y se comparan los dos hashes. Si coinciden, la contraseña es correcta.

**BCrypt** es el algoritmo que usamos. Tiene dos propiedades clave:
- **Sal (salt) automática:** agrega datos aleatorios, así que la misma contraseña da hashes distintos en
  cada usuario. Sin sal, dos usuarios con la misma contraseña tendrían el mismo hash y se podría usar una
  tabla precalculada.
- **Es lento a propósito:** tarda ~100 ms. Para un usuario es imperceptible; para alguien que prueba
  millones de contraseñas, es una barrera enorme.

**En nuestro código:**
```java
String hashedPassword = passwordEncoder.encode(request.getPassword());   // al registrarse
boolean ok = passwordEncoder.matches(request.getPassword(), user.getPassword());  // al iniciar sesión
```

---

## 2.4 CORS: por qué el navegador bloquea peticiones

**El problema:** una página maliciosa (`sitio-malo.com`) hace una petición a `tu-banco.com`. Si el navegador
enviara **automáticamente** tus cookies del banco, el sitio malo podría leer tu saldo o transferir plata.
Para evitarlo existe la **Same-Origin Policy**: por defecto, una página solo puede pedir datos a su propio
origen.

**Qué es CORS:** un mecanismo por el cual **el servidor le dice al navegador** qué otros orígenes tienen
permitido consultarlo. "Origen" = protocolo + dominio + puerto.

**Cómo funciona el *preflight*:** cuando el navegador ve una petición "no simple" (por ejemplo, con la
cabecera `Authorization`), **primero manda una petición `OPTIONS`** preguntando *"¿puedo?"*. El servidor
responde con las reglas. Solo si el navegador queda conforme, manda la petición real.

> ⚠️ **Por eso en `SecurityConfig` eximimos `OPTIONS` de la autenticación.** El preflight **no lleva el
> token** (el navegador todavía no sabe si puede mandar la petición). Si exigiéramos autenticación,
> respondería 403 y el navegador **nunca** haría la petición real.

**En nuestro caso:** la app móvil **no usa CORS** (las aplicaciones nativas no mandan cabecera `Origin`).
Solo aplica a un cliente web. Dejamos los orígenes **configurables** con la variable `CORS_ALLOWED_ORIGINS`.

---

## 2.5 CSRF: el ataque que NO nos afecta (y por qué desactivamos la protección)

**Qué es CSRF** (*Cross-Site Request Forgery*): te hacés una petición a `tu-banco.com` **sin darte cuenta**.
Estás logueado en el banco (tenés la cookie), entrás a `sitio-malo.com`, y ese sitio hace
`<img src="https://tu-banco.com/transferir?monto=1000">`. Tu navegador manda la petición **con tu cookie
puesta automáticamente** y la transferencia se ejecuta.

**La clave:** CSRF vive de que el navegador mande las **credenciales automáticamente** (cookies).

**Por qué no nos afecta:** nuestra API **no usa cookies**. La autenticación viaja en la cabecera
`Authorization: Bearer <token>`, y **ningún navegador agrega esa cabecera por su cuenta**. Un sitio malicioso
no puede forzarla. Sin ese mecanismo, el ataque no existe.

**Por eso** desactivamos la protección (`csrf().disable()`), que es lo que **recomienda la documentación de
Spring Security para APIs sin estado con token**.

> **Dato para la defensa:** CodeQL marcó esto como severidad **alta**. Es un **falso positivo en contexto**:
> el analizador ve la línea que desactiva la protección, pero no conoce la arquitectura. Explicar esto
> demuestra criterio técnico.

---

## 2.6 Vulnerabilidades: CVE y CVSS

**CVE** (*Common Vulnerabilities and Exposures*): un **identificador único** para una vulnerabilidad conocida.
Formato: `CVE-2026-5450`. Lo administra **MITRE**.

**No es una herramienta: es un estándar de nomenclatura.** Existe para que un reporte de Grype, uno de Trivy
y uno de Snyk hablen de **la misma falla** con el mismo nombre.

**CVSS** (*Common Vulnerability Scoring System*): el **puntaje de 0.0 a 10.0** que mide la gravedad.

| Rango | Severidad |
| :--- | :--- |
| 0.0 – 3.9 | Baja |
| 4.0 – 6.9 | Media |
| 7.0 – 8.9 | Alta |
| **9.0 – 10.0** | **Crítica** |

**Tampoco es una herramienta: es un estándar de medición.** Sirve para **priorizar**: no se puede arreglar
todo a la vez, entonces se empieza por lo crítico.

> **La analogía:** el CVE es el **número de expediente** de un delito; el CVSS es la **gravedad** de ese
> delito; y Grype, Trivy o Snyk son los **peritos** que investigan y te traen el expediente.

**GHSA** es lo mismo pero del sistema de avisos de GitHub (`GHSA-xxxx-xxxx-xxxx`), y es el formato que usa
Grype para las dependencias Java.

**OWASP** es una fundación sin fines de lucro que publica guías de seguridad (el "Top 10" de riesgos web es
suyo). De ahí viene la librería `owasp-java-encoder` que usamos para neutralizar los logs.

---
---

# PARTE 3 — CONTENEDORES

## 3.1 Docker: el problema que resuelve

**El problema clásico:** *"en mi máquina funciona"*. Tu aplicación anda en tu computadora con Java 26, pero
en el servidor hay Java 17 y no arranca. O falta una librería, o la versión de MySQL es distinta.

**Qué es Docker:** empaqueta **la aplicación junto con TODO lo que necesita** (el runtime, las librerías, la
configuración) en un paquete único y aislado. Ese paquete corre **igual** en cualquier lado donde haya Docker.

**Analogía:** en vez de mandar los ingredientes sueltos y una receta (que puede salir distinto en cada
cocina), mandás **la comida ya cocinada en un tupper hermético**.

**Los dos conceptos que se confunden siempre:**

| | **Imagen** | **Contenedor** |
| :--- | :--- | :--- |
| Qué es | El **molde**: un paquete inmutable con el sistema de archivos y las instrucciones de arranque | La instancia **en ejecución** de ese molde |
| Analogía | El **plano** o la **receta impresa** | La **casa construida** o el **plato servido** |
| Estado | **Inmutable** — no cambia nunca | Efímero — se puede crear, detener y borrar |
| Cantidad | Una | Muchas copias de la misma imagen |
| Comando | `docker build` | `docker run` |

De **una** imagen podés crear **muchos** contenedores, cada uno aislado.

**Por qué se dice que las imágenes son inmutables:** no se "edita" una imagen: se construye una nueva. Eso es
lo que permite tener versiones (`v1.0.1`, `v1.0.2`) y poder **volver atrás** desplegando una anterior.

---

## 3.2 El Dockerfile y las capas

**Qué es:** un archivo de texto con las **instrucciones** para construir una imagen. Se lee de arriba hacia
abajo; cada instrucción crea una **capa**.

**El concepto de capa y la caché:** Docker guarda cada capa y la **reutiliza** si nada de lo anterior cambió.
Esto hace que las construcciones sean mucho más rápidas.

**Ejemplo aplicado** — por qué copiamos los archivos de build **antes** que el código:

```dockerfile
COPY gradlew .                          # capa 1: cambia casi nunca
COPY gradle/ gradle/                    # capa 2
COPY settings.gradle.kts build.gradle.kts ./   # capa 3: cambia si toco dependencias
RUN ./gradlew dependencies              # capa 4: DESCARGA TODAS LAS DEPENDENCIAS
COPY src/ src/                          # capa 5: cambia en CADA modificación de código
RUN ./gradlew bootJar                   # capa 6: compila
```

Si copiáramos todo el código de una sola vez (capa 5), cualquier cambio en un archivo `.java` invalidaría la
capa 4 y **volvería a descargar todas las dependencias**. Copiando en este orden, mientras no cambien las
dependencias, la descarga se reutiliza.

---

## 3.3 Construcción multietapa (*multi-stage*)

**El problema:** para **compilar** Java necesitás el JDK y Gradle (≈ 700 MB). Para **ejecutar** solo necesitás
la JRE (≈ 200 MB). Si usás una sola imagen, te llevás todo el peso de las herramientas de compilación… **y
también todas sus vulnerabilidades**.

**Qué es:** un Dockerfile con **varios `FROM`**. El primero compila; el último es el que queda. Y se puede
**copiar archivos de una etapa a otra**.

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS builder     # ETAPA 1: compila
RUN ./gradlew bootJar

FROM gcr.io/distroless/java21-debian12:nonroot   # ETAPA 2: ejecuta
COPY --from=builder /workspace/build/libs/app.jar app.jar
```

**Resultado:** la imagen final tiene **solo el JAR y una JRE**. Nada de Gradle, ni código fuente, ni
compiladores, ni gestor de paquetes.

**Analogía:** es como mudarte: usás una camioneta de carga (etapa 1) para llevar todo, pero en la casa nueva
solo entran los muebles (etapa 2). La camioneta se va.

---

## 3.4 Distroless y usuario no privilegiado

**Distroless** ("sin distribución") son imágenes que contienen **únicamente el runtime**, sin el sistema
operativo completo. Concretamente, **no tienen**:
- **Shell** (`sh`, `bash`) → no podés "entrar" a explorar
- **Gestor de paquetes** (`apt`) → no podés instalar herramientas
- **Compilador de C**, ni utilidades del sistema

**Por qué importa (dos razones que se preguntan):**

1. **Seguridad:** si un atacante logra ejecutar código, **no tiene con qué moverse**. No puede abrir una
   terminal, ni descargar un programa, ni compilar nada.
2. **Menos vulnerabilidades:** Grype escanea los paquetes del sistema. Si no existen, no aparecen CVEs. Con
   una imagen `ubuntu:latest` tendríamos **más de 400 CVEs solo del sistema operativo**; con distroless,
   muchas menos.

**Usuario no privilegiado:** por defecto, los contenedores corren como **root**. Nosotros usamos
`USER nonroot:nonroot`, así que el proceso corre con un usuario **sin privilegios** (UID 65532). Si alguien
explotara una vulnerabilidad, no podría escalar privilegios ni escribir fuera de su carpeta.

> **Prueba para demostrarlo en el parcial:** `docker run --rm <imagen> sh` **debe fallar** con
> "executable file not found". Si abriera una shell, la imagen no sería distroless y se perdió la ventaja.

---

## 3.5 Docker Compose

**El problema:** la aplicación sola no sirve: necesita una base de datos. Habría que crear una red, arrancar
dos contenedores conectados, configurar el volumen, verificar el orden… Comandos `docker run` larguísimos.

**Qué es:** una herramienta para describir **varios contenedores** en un solo archivo `docker-compose.yml` y
levantarlos juntos con un comando.

```yaml
services:
  db:       # MySQL
  backend:  # Spring Boot
```

**Diferencia clave con Dockerfile** (pregunta clásica):

| | **Dockerfile** | **Docker Compose** |
| :--- | :--- | :--- |
| Define | Cómo **construir una imagen** | Cómo **ejecutar contenedores** |
| Responde | "¿Qué contiene mi app y cómo arranca?" | "¿Qué levanto y cómo se conecta?" |
| Comando | `docker build` | `docker compose up` |

**Tres detalles de nuestro Compose que vale la pena entender:**

1. **`depends_on` con `condition: service_healthy`:** no alcanza con que el contenedor de MySQL
   **exista**, tiene que estar **listo** para aceptar conexiones. Sin esta condición, Spring Boot arrancaría
   antes de que MySQL esté preparado y moriría. **Este es exactamente el error que vivimos en Azure** (el
   HTTP 503).
2. **`jdbc:mysql://db:3306`:** dentro de la red de Compose, cada servicio se resuelve por **su nombre**
   (`db`), no por `localhost`. `localhost` apuntaría al propio contenedor del backend, donde no hay base.
3. **Volumen con nombre:** los datos **sobreviven** a `docker compose down`. Sin volumen, cada reinicio
   empezaría con la base vacía.

---

## 3.6 ¿Qué es un registro de imágenes?

**El problema:** la imagen está en tu computadora, pero el servidor de Azure está en Brasil. ¿Cómo llega?

**Qué es:** un **registro** es un repositorio de imágenes **en la nube**. Es como GitHub pero para imágenes
Docker. Vos **subís** (`push`) y el servidor de destino **baja** (`pull`).

**Docker Hub** es el público (como Instagram para imágenes). **Azure Container Registry (ACR)** es el privado
de Azure. Nosotros usamos ACR.

**Etiquetas (tags):** una misma imagen puede tener varios nombres. `interview-backend:v1.1.0`,
`interview-backend:6aec073`… Sirven para **identificar versiones**. Sin etiquetas distintas, no podrías saber
qué código está corriendo.

> ⚠️ **Nunca desplegar `:latest`.** Es una etiqueta móvil: cambia con cada push y no dice nada. Si desplegás
> `:latest`, no podés responder *"¿qué versión está corriendo?"* ni volver a una anterior con precisión.

---
---

# PARTE 4 — LA NUBE Y AZURE

## 4.1 ¿Qué es "la nube"?

**El problema:** tener un servidor propio implica comprarlo, instalarlo, mantenerlo, actualizarlo, y pagarlo
aunque no lo uses. Para un proyecto chico o una startup, es inviable.

**Qué es:** usar **computadoras de otra empresa**, alquiladas por minuto y administradas por ellos.

**Los tres modelos:**

| Modelo | Qué manejás vos | Ejemplo |
| :--- | :--- | :--- |
| **IaaS** | Te dan la máquina; vos instalás todo | Una máquina virtual (VM) |
| **PaaS** | Te dan la plataforma; vos solo subís tu aplicación | **Azure Container Apps** ← lo nuestro |
| **SaaS** | Te dan el programa listo | Gmail |

**Regiones:** los proveedores tienen **centros de datos en todo el mundo**. Elegís la más cercana para que
haya menos latencia. Nosotros usamos **`brazilsouth`** (San Pablo) por cercanía a Argentina.

---

## 4.2 La jerarquía de Azure ⭐

Esta estructura **hay que saberla de memoria**:

```
SUSCRIPCIÓN (Azure for Students)
  │  Es el "contrato" y el ámbito de facturación. Acá está el crédito.
  │
  └─ GRUPO DE RECURSOS (rg-interview-app)
       │  Es una CARPETA lógica para agrupar recursos relacionados.
       │  Sirve para administrarlos juntos y ¡borrarlos juntos!
       │
       ├─ Azure Container Registry (acrvaniainterview)
       │     └─ Imágenes Docker versionadas
       │
       ├─ Azure Database for MySQL (mysql-interview-backend)
       │     └─ La base de datos gestionada
       │
       ├─ Container Apps Environment (env-interview-app)
       │     └─ Container App (interview-backend)
       │           └─ La aplicación corriendo
       │
       └─ Log Analytics (workspace-rginterviewappMrgY)
             └─ Los logs de la aplicación
```

**Analogía:** la **suscripción** es tu cuenta bancaria; el **grupo de recursos** es una carpeta de
presupuesto; los **recursos** son los gastos dentro de esa carpeta.

**Por qué el grupo de recursos es tan útil:** se puede **borrar entero** y se elimina todo lo que contiene.
Es la forma correcta de terminar un proyecto y **dejar de pagar** de una sola vez.

---

## 4.3 Azure Container Registry (ACR)

**Qué es:** el registro privado de imágenes de Azure. Es nuestro "Docker Hub privado".

| Dato | Valor nuestro |
| :--- | :--- |
| Nombre | `acrvaniainterview` |
| Dirección | `acrvaniainterview.azurecr.io` |
| SKU | **Basic** (el más barato, ~USD 5/mes) |
| Dónde vive | `rg-interview-app`, región `brazilsouth` |

**Qué guarda:** las imágenes `interview-backend` con sus etiquetas (`v1.1.0`, `6aec073`, etc.).

**Autenticación:** se puede con el **usuario admin del registro** (una contraseña fija) o con el **Service
Principal**. Nosotros usamos el Service Principal, que es más seguro.

---

## 4.4 Azure Container Apps (ACA)

**Qué es:** un servicio **PaaS** que ejecuta contenedores sin que administres servidores ni Kubernetes.

| Concepto | Qué es |
| :--- | :--- |
| **Environment** | El "vecindario" donde viven las aplicaciones. Agrupa recursos compartidos y, en nuestro caso, la conexión a Log Analytics |
| **Container App** | La aplicación en sí |
| **Revision** | Cada versión desplegada. Al actualizar la imagen, se crea una **revisión nueva** |
| **Ingress** | La "puerta de entrada": expone la aplicación a internet con HTTPS y dominio propio |
| **Scale** | Cuántas réplicas correr |

**Las tres propiedades que hacen de ACA una buena elección:**

1. **Escala a cero (`minReplicas: 0`):** si nadie usa la aplicación, **no hay ninguna instancia corriendo y no
   se paga nada**. Cuando llega una petición, arranca sola (tarda unos segundos, es el *cold start*).
2. **HTTPS y dominio incluidos:** Azure te da una URL
   (`interview-backend.happybay-....brazilsouth.azurecontainerapps.io`) con certificado, sin que configures
   nada.
3. **Sondas de salud:** Azure consulta periódicamente los endpoints de *liveness* y *readiness* para saber si
   el contenedor está vivo y si puede recibir tráfico.

> **Pregunta que pueden hacer:** *"¿qué pasa si la aplicación tarda en arrancar?"* → Azure espera a que la
> sonda de *readiness* responda OK antes de mandarle tráfico. Mientras tanto devuelve **503 "App port is not
> ready"** — que es exactamente el error que tuvimos.

---

## 4.5 Service Principal ⭐

**El problema:** GitHub Actions necesita **autenticarse en Azure** para publicar imágenes y desplegar. ¿Le das
tu usuario y contraseña personales? **No.** Tendría acceso a todo y, si te cambiás la contraseña, se rompe.

**Qué es:** una **identidad para programas**, no para personas. Es como un "usuario robot" con su propio
usuario y contraseña, al que se le asignan **solo los permisos que necesita**.

**Sus cuatro datos:**

| Dato | Qué es |
| :--- | :--- |
| `clientId` | El "nombre de usuario" del robot |
| `clientSecret` | La "contraseña" del robot |
| `tenantId` | El directorio (organización) donde existe |
| `subscriptionId` | La suscripción sobre la que va a operar |

**Los cuatro juntos forman el JSON que se guarda en el secreto `AZURE_CREDENTIALS` de GitHub.**

**Roles y permisos (mínimo privilegio):** un rol es un **conjunto de permisos** que se asigna sobre un
**ámbito** (scope). Nosotros le dimos a nuestro SP exactamente dos:

| Rol | Ámbito | Para qué |
| :--- | :--- | :--- |
| `AcrPush` | Solo el ACR | Subir imágenes |
| `Container Apps Contributor` | Solo la Container App | Desplegar |

> ⭐ **Nosotros empezamos con `Contributor` sobre TODO el grupo de recursos** (el rol más amplio) y después lo
> **reducimos**. Eso se llama aplicar el **principio de mínimo privilegio**: que cada identidad tenga solo los
> permisos necesarios, ni uno más. Es una de las cosas que más se valora en seguridad.

> **Dato para la defensa:** hay una alternativa más moderna llamada **OIDC** (federación de identidad), donde
> GitHub y Azure se "confían" mutuamente y **no hace falta guardar ningún secreto**. Es más segura porque no
> hay contraseña que se pueda filtrar o vencer. No la usamos porque la cátedra trabajó con Service Principal
> y credencial, pero conviene mencionarla como mejora.

---

## 4.6 Variables de entorno y secretos

**El problema:** la contraseña de la base no puede estar escrita en el código (el repositorio es público y el
código se copia a todos lados). Pero la aplicación la necesita.

**Solución:** **separar la configuración del código**. El código dice *"necesito una variable llamada
`SPRING_DATASOURCE_PASSWORD`"*, y el valor se lo inyectan **desde afuera** al arrancar.

La sintaxis que usamos en `application.properties`:
```properties
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}
```
Se lee: *"usá la variable de entorno; si no existe, usá `root`"*. El `root` es solo para desarrollo local.

**Dónde vive cada valor según el entorno:**

| Entorno | Dónde están los valores |
| :--- | :--- |
| Desarrollo local | Archivo `.env` (que **no se sube** a git) |
| Producción (Azure) | **Container App Secrets** |
| Para desplegar (GitHub) | **GitHub Secrets** |

> ⭐ **Concepto clave que preguntan: hay DOS TIPOS de credenciales y NO se mezclan.**
>
> | | Quién las usa | Para qué |
> | :--- | :--- | :--- |
> | **Service Principal** | GitHub Actions | Desplegar |
> | **Credenciales de la app** (contraseña MySQL, `JWT_SECRET`) | Spring Boot al ejecutarse | Conectarse a la base y firmar tokens |
>
> GitHub **nunca ve** la contraseña de MySQL, y Spring Boot **nunca ve** el secreto del Service Principal.

**Una buena práctica en el código:** en las variables del Container App, las sensibles no se guardan como
texto sino que **apuntan a un secreto**:
```
SPRING_DATASOURCE_PASSWORD = secretref:db-password
```
Se lee: *"andá a buscar el secreto llamado `db-password`"*. Así el valor real **no aparece** en la definición
del contenedor.

---
---

# PARTE 5 — CI/CD

## 5.1 ¿Qué es integración continua (CI)?

**El problema:** si cada uno del equipo trabaja semanas en su rama y después intenta unir todo, aparecen
conflictos gigantes y errores que nadie probó. *"Integración"* es el momento de unir el código de todos.

**Qué es CI:** la práctica de **integrar los cambios seguido** (varias veces al día) y **verificar
automáticamente cada integración**. Que no sea una persona la que se acuerde de correr los tests, sino que lo
haga un robot en cada cambio.

**Nuestro CI hace, en cada push:**

```
1. Descargar el código
2. Instalar Java 21
3. Compilar y EJECUTAR LOS TESTS (contra un MySQL real y descartable)
4. Construir la imagen Docker
5. Escanearla con Grype buscando vulnerabilidades
6. Autenticarse en Azure
7. Publicar la imagen en el ACR
```

**El valor:** si los tests fallan, **el pipeline se detiene y no publica nada**. Nunca llega una imagen rota
al registro.

> **Esto lo vivimos de verdad:** la primera ejecución falló en el paso de compilación. El pipeline **no
> publicó nada** y nos avisó. Si no existiera el CI, ese error habría llegado hasta el despliegue.

---

## 5.2 ¿Qué es despliegue continuo (CD)?

**Qué es:** llevar automáticamente a producción cada versión que pasó las verificaciones.

**Diferencia entre "entrega continua" y "despliegue continuo":**
- **Entrega continua:** cada versión queda **lista** para desplegar, pero alguien aprieta el botón.
- **Despliegue continuo:** se despliega **solo**, sin intervención.

**Nuestra decisión (y es el punto más importante de este proyecto):** **desacoplamos el CI del CD a
propósito.** Publicar una imagen y liberarla a producción son **dos cosas distintas**.

> **Las tres frases que resumen el proyecto:**
> 1. **"La construcción de imágenes es exclusiva del pipeline"**
> 2. **"Publicar no es liberar"**
> 3. **"El pipeline verifica que la aplicación responda de verdad"**

**Los tres casos que podemos demostrar:**

| Caso | Cómo se logra | Qué pasa |
| :--- | :--- | :--- |
| **A** | Push a `main` | Construye, escanea y publica la imagen. **NO despliega** |
| **B** | Push de una etiqueta `v1.2.3` | Lo mismo **+ despliega** automáticamente |
| **C** | *Run workflow* + casilla "deploy" | Igual, pero **decidido a mano**. Permite elegir qué versión desplegar |

---

## 5.3 GitHub Actions: cómo funciona

**Qué es:** el sistema de automatización de GitHub. Ejecuta tareas cuando pasa algo en el repositorio.

**Los cinco términos que hay que saber:**

| Término | Qué es |
| :--- | :--- |
| **Workflow** | El archivo `.yml` con todo el proceso. Nuestro archivo es `.github/workflows/ci.yml` |
| **Evento** (*trigger*) | Qué lo dispara: un push, un tag, o manualmente |
| **Job** | Un conjunto de pasos. Nuestro pipeline tiene **dos**: `build-scan-push` y `deploy` |
| **Step** | Cada tarea individual dentro de un job |
| **Runner** | La máquina virtual (Ubuntu) donde GitHub ejecuta todo. **Es descartable: se crea y se destruye en cada ejecución** |

**¿Por qué el runner es descartable y por qué eso importa?** Porque significa que cada ejecución empieza
**desde cero**: no hay nada instalado de la vez anterior. Por eso el pipeline tiene que instalar Java, las
dependencias y todo lo que necesita cada vez.

**Los *Artifacts*:** archivos que el pipeline produce y quedan guardados para descargar (el informe de
vulnerabilidades, el resultado de los tests).

**Los *Secrets*:** valores cifrados que el pipeline lee en tiempo de ejecución. GitHub los **enmascara** en
los logs: si aparecen, los muestra como `***`.

**Tipos de trigger que usamos:**

```yaml
on:
  push:
    branches: [main]        # cuando se sube código a main
    tags: ['v*.*.*']        # cuando se sube una etiqueta de versión
  workflow_dispatch:        # manual, desde el botón "Run workflow"
    inputs:
      deploy: ...           # con una casilla para decidir si desplegar
```

---

## 5.4 Condiciones: el `if` que controla el despliegue

El job de despliegue no corre siempre. Tiene una **condición**:

```yaml
deploy:
  needs: build-scan-push                              # depende del CI: si falla, no corre
  if: >-
    needs.build-scan-push.result == 'success' &&
    (startsWith(github.ref, 'refs/tags/v') || inputs.deploy == true)
```

Se lee: **"desplegar solo si el CI terminó bien Y (se subió una etiqueta de versión O se pidió
manualmente)"**.

Un push normal a `main` no cumple ninguna de las dos → el job aparece como **`skipped`** (gris en la
interfaz). **Eso es el Caso A.**

---

## 5.5 Estrategia de versionado de imágenes ⭐

**El problema que corregimos:** en el proyecto de referencia (en Go), el script manual usaba **un criterio de
etiquetado** y el pipeline **otro**. Resultado: podías desplegar sin saber qué código estaba corriendo.

**Nuestra regla es única:**

| Etiqueta | Cuándo se genera | Para qué sirve |
| :--- | :--- | :--- |
| **`:<sha-corto>`** (ej. `6aec073`) | **Siempre** | **Trazabilidad total**: es el identificador del commit exacto que generó esa imagen |
| **`:vX.Y.Z`** (ej. `v1.1.0`) | Cuando el push es de una etiqueta de git | Versión semántica legible para humanos |
| **`:latest`** | Siempre | Solo comodidad. **El despliegue NUNCA lo usa** |

**Qué es el "SHA"**: cada commit en git tiene un identificador único calculado a partir de su contenido (40
caracteres hexadecimales). El "sha corto" son los primeros 7.

**Por qué dos criterios a la vez:** el `:v1.1.0` es legible para las personas ("la versión 1.1.0"); el
`:6aec073` es **exacto** para la máquina. Con ambos podés responder *"¿qué versión corre?"* y *"¿qué commit
exacto generó esta imagen?"*.

**Versionado semántico (SemVer):** `MAYOR.MENOR.PARCHE` → `1.1.0`
- **MAYOR** cuando hay cambios incompatibles
- **MENOR** cuando agregás funcionalidad compatible (nuestro `v1.1.0` agregó el despliegue continuo)
- **PARCHE** cuando corregís errores

---

## 5.6 Por qué un pipeline puede fallar (y qué hacer)

| Paso que falla | Causa típica | Qué hacer |
| :--- | :--- | :--- |
| Compilación / tests | Error de código, test roto | Reproducir en local con `./gradlew build` |
| Login en Azure | El secreto `AZURE_CREDENTIALS` venció o está mal | Regenerar la credencial del Service Principal |
| Push al ACR | Al Service Principal le falta el rol `AcrPush` | Revisar los roles |
| Verificación (health) | La aplicación no arranca — **típicamente, la base está apagada** | Encender la base y reintentar |

**Cómo se reintenta:** botón **"Re-run all jobs"** en la ejecución fallida.

> **Caso real:** la primera ejecución falló con `Permission denied` al ejecutar `./gradlew`. La causa **no era
> el código**: en Windows, git no guarda el permiso de ejecución de los archivos, así que `gradlew` quedó en
> el repositorio como **no ejecutable** y Linux no lo podía correr. Se arregló con
> `git update-index --chmod=+x gradlew`, que corrige el permiso **en el repositorio** (no un parche en el
> pipeline).

---
---

# PARTE 6 — OPERACIÓN

## 6.1 Observabilidad: logs, métricas y salud

**El problema:** la aplicación está corriendo en un servidor en Brasil. **No la podés ver.** ¿Cómo sabés si
funciona?

**Las tres herramientas responden preguntas distintas:**

| Herramienta | Responde | Ejemplo |
| :--- | :--- | :--- |
| **Logs** | *¿Qué pasó?* (el detalle de cada evento) | "A las 18:32 falló la conexión a la base" |
| **Métricas** | *¿Cuánto, cuántas veces, cuán rápido?* (tendencias) | "Se recibieron 2 peticiones en la última hora" |
| **Health checks** | *¿Está vivo y listo AHORA?* | `{"status":"UP"}` |

> ⚠️ **Error conceptual frecuente:** los logs **son solo una parte** de la observabilidad, **no toda**. Si
> solo mirás logs, no podés ver tendencias ni saber si el servicio está disponible en este instante.

**Nuestras tres capas:**

```
CAPA 1 — La aplicación (Spring Boot Actuator)
         Métricas internas: tiempos de respuesta, pool de conexiones, memoria de la JVM

CAPA 2 — La plataforma (Azure Monitor)
         Métricas del contenedor: peticiones, réplicas, reinicios, CPU, memoria, métricas de JVM

CAPA 3 — Los logs (Log Analytics)
         Todos los mensajes que la aplicación escribe, consultables con un lenguaje de consulta
```

---

## 6.2 Spring Boot Actuator

**Qué es:** un módulo de Spring Boot que **expone endpoints para observar la aplicación**. Se agrega como
dependencia y funciona solo.

| Endpoint | Devuelve |
| :--- | :--- |
| `/actuator/health` | Estado: `UP` / `DOWN`. Con autenticación, el detalle de cada componente |
| `/actuator/info` | Versión y fecha de build |
| `/actuator/metrics` | Todas las métricas: tiempos HTTP, pool de conexiones, JVM |

**Micrometer** es la librería que está detrás: es una "cara" que expone métricas de forma estándar, sin que
vos tengas que instrumentar nada.

**La decisión de seguridad que tomamos:**

```
Sin token:  {"status":"UP"}                       ← solo el estado
Con token:  {"status":"UP","components":{...}}    ← detalle: base de datos, disco, etc.
```

**Por qué:** publicar que usás MySQL, cuánto espacio libre hay y qué componentes tenés **le sirve a un
atacante** para preparar un ataque, y no le aporta nada a un usuario anónimo.

**Por qué el endpoint sigue siendo público:** lo consultan las **sondas de Azure** (para saber si el
contenedor está vivo) y el **pipeline** (para verificar que la versión recién desplegada funciona).

---

## 6.3 *Liveness* vs *Readiness*: iniciada no es lo mismo que lista

**Concepto clave:** una aplicación puede estar **iniciada** (el proceso arrancó, el puerto está abierto) pero
**todavía no lista** (los componentes internos no terminaron de inicializarse).

- **Liveness** (¿está viva?) → si falla, Azure **reinicia** el contenedor
- **Readiness** (¿puede recibir tráfico?) → si falla, Azure **no le manda peticiones**

**Nuestro caso real:** Azure devolvía **503 "App port is not ready"**. Los logs mostraron la cadena:

```
Connection refused → Communications link failure → JPA EntityManagerFactory → Unable to start embedded Tomcat
```

Traducido: Spring moría al crear la conexión a la base porque **faltaban las variables de entorno**.
El contenedor existía, pero la aplicación nunca terminaba de arrancar. **Eso es exactamente "iniciada pero no
lista".**

Y esto **lo usamos en el pipeline**: después de desplegar, GitHub consulta el health hasta obtener un 200.
**El pipeline no se declara exitoso hasta comprobar que la aplicación responde de verdad.** Un
`az containerapp update` que responde "OK" solo dice que Azure **aceptó el comando**, no que la app funcione.

---

## 6.4 Los 12 factores ⭐

Son **12 buenas prácticas** para construir aplicaciones que funcionen bien en la nube. Estas son las que
aplicamos:

| Factor | Qué dice | Cómo lo aplicamos |
| :--- | :--- | :--- |
| **III. Config** | La configuración va en **variables de entorno**, no en el código | `application.properties` solo tiene marcadores `${VARIABLE:default}` |
| **V. Build, release, run** | Separar **construir**, **liberar** y **ejecutar** | El pipeline construye, publica una versión inmutable en el ACR, y el despliegue la ejecuta. **Se puede construir sin liberar (Caso A)** |
| **VII. Port binding** | El servicio **expone su propio puerto** | Spring Boot levanta Tomcat en el 8080 por sí mismo, sin servidor externo |
| **XI. Logs** | Los logs son un **flujo de eventos**: la aplicación escribe a `stdout`, no a archivos | El contenedor es efímero y no acumula estado; Azure captura `stdout` y lo manda a Log Analytics |

**Por qué el factor V es el más importante para nuestro proyecto:** es la justificación técnica de que
**publicar no es liberar**. Son etapas distintas y se pueden controlar por separado.

---

## 6.5 Análisis de seguridad: Grype y CodeQL

**El problema:** las aplicaciones usan **decenas de librerías de terceros**. Si una tiene una vulnerabilidad
conocida, la tuya también. ¿Cómo te enterás?

### Grype — análisis de dependencias e imagen

**Qué hace:** mira **todo lo que hay dentro de tu imagen Docker** (librerías Java, paquetes del sistema
operativo, binarios) y lo compara contra **bases de datos de vulnerabilidades conocidas**.

**Cómo se lee su informe:**

```
NAME              INSTALLED   FIXED-IN   VULNERABILITY        SEVERITY
tomcat-embed-core 10.1.43     10.1.55    GHSA-r29c-68gh-xp6x  Critical
spring-webmvc     6.2.19      -          GHSA-pc63-qcmh-9cmg  Critical
```

La columna **`FIXED-IN` es la que manda la decisión**:
- Si hay versión de corrección → **se actualiza**
- Si dice `-` (sin fix) → **se documenta y se mitiga**, porque no hay nada que hacer todavía

**Nuestros resultados:** de **186** vulnerabilidades bajamos a **115** (−38%), y las **críticas de 10 a 3**.
Las 3 que quedan **no tienen corrección publicada** y están justificadas en la documentación. **No se
esconden.**

### CodeQL — análisis estático del código

**Qué hace:** analiza **el código que escribimos nosotros** buscando **patrones inseguros**: inyección SQL,
XSS, inyección de registros, criptografía débil.

**Diferencia clave:**

| | **Grype** | **CodeQL** |
| :--- | :--- | :--- |
| Analiza | Dependencias e imagen | **Código propio** |
| Responde | "¿Esta librería tiene una CVE publicada?" | "¿Este código nuestro tiene un patrón vulnerable?" |
| Riesgo que cubre | **Heredado** (de terceros) | **Propio** (lo que escribimos) |

**Son complementarios.** Grype no encuentra lo que escribimos mal; CodeQL no sabe de librerías.

> ⭐ **Precisión que hay que hacer:** CodeQL es **análisis estático** — **NO bloquea ataques en tiempo de
> ejecución**. Solo advierte que el patrón existe en el código, antes de que se despliegue. Quien evita la
> inyección SQL en ejecución son las **consultas parametrizadas**.

**Lo que encontró y lo que aprendimos:**

| Alerta | Veredicto |
| :--- | :--- |
| CSRF desactivado (alta) | **Falso positivo**: no usamos cookies, el ataque no aplica |
| Inyección de registros ×3 (media) | **Reales**: corregidas con OWASP Java Encoder |

**Y una lección enorme:** la primera corrección que hicimos (con una expresión regular propia) **funcionaba
perfectamente**, pero CodeQL **abrió las mismas alertas de nuevo** porque **no reconoce sanitizadores que no
conoce**. Es una **limitación esperable del análisis estático**: el analizador no ejecuta el programa, no
puede deducir la semántica de un código arbitrario.

> **Frase para la defensa:** *"Las herramientas de análisis estático **proponen**, no deciden. Un hallazgo
> puede ser un falso positivo, y una corrección válida puede no ser reconocida. El criterio técnico humano es
> el que cierra la decisión."*

---
---

# PARTE 7 — EL RECORRIDO COMPLETO

Esta es la historia de una línea de código, desde que la escribís hasta que responde en internet. **Si
entendés esto, entendés todo el proyecto.**

```
1. ESCRIBÍS EL CÓDIGO en tu computadora
   ↓
2. LO PROBÁS LOCALMENTE con Docker Compose
   ├─ docker compose levanta dos contenedores: Spring Boot + MySQL
   └─ Verificás: curl http://localhost:8080/actuator/health → UP
   ↓
3. HACÉS COMMIT Y PUSH A GITHUB
   git add . && git commit -m "feat: ..." && git push origin main
   ↓
4. GITHUB ACTIONS SE DESPIERTA SOLO (el evento push dispara el workflow)
   Corre en un runner Ubuntu descartable:
   ├─ Descarga tu código
   ├─ Instala Java 21
   ├─ Compila y corre los tests (levanta un MySQL temporal para los tests)
   ├─ Construye la imagen Docker (multietapa → distroless)
   ├─ La escanea con Grype
   └─ Se autentica en Azure con el Service Principal
   ↓
5. PUBLICA LA IMAGEN EN EL ACR con dos etiquetas: :6aec073 y :latest
   ↓
6. ¿DESPLEGAR? Solo si se cumplió la condición:
   ├─ Push normal a main        → NO  (Caso A) ← aparece como "skipped"
   ├─ Push de tag v1.2.3        → SÍ  (Caso B)
   └─ Botón manual con deploy   → SÍ  (Caso C)
   ↓
7. DESPLIEGA: az containerapp update --image .../interview-backend:v1.1.0
   Azure crea una REVISIÓN NUEVA con esa imagen
   ↓
8. VERIFICA: el pipeline consulta el health público hasta obtener HTTP 200
   (si no responde, el pipeline falla y te avisa)
   ↓
9. LA APLICACIÓN RECIBE TRÁFICO
   Cliente → HTTPS → Azure (ingress) → contenedor (puerto 8080) → Spring Boot
                                            ↓
                         JwtAuthFilter valida el token
                                            ↓
                         Controller → Service → Repository
                                            ↓
                              HikariCP → MySQL (con SSL)
   ↓
10. TODO QUEDA REGISTRADO
    ├─ Los logs van a Log Analytics (consultables con KQL)
    ├─ Las métricas van a Azure Monitor (peticiones, réplicas, reinicios, JVM)
    └─ Actuator expone las métricas internas (tiempos, pool, memoria)
```

---

# PARTE 8 — GLOSARIO

| Término | Qué es |
| :--- | :--- |
| **API** | Conjunto de "puertas de entrada" a un sistema |
| **ACR** | Azure Container Registry: registro privado de imágenes |
| **ACA** | Azure Container Apps: servicio que ejecuta contenedores |
| **Artifact** | Archivo que produce el pipeline y queda para descargar |
| **BCrypt** | Algoritmo de hash de contraseñas, con sal y lento a propósito |
| **Bean** | Objeto que Spring crea y administra |
| **CI** | Integración Continua: verificar cada cambio automáticamente |
| **CD** | Despliegue Continuo: llevar a producción automáticamente |
| **Claim** | Cada dato dentro de un JWT (`sub`, `exp`, etc.) |
| **Contenedor** | Instancia en ejecución de una imagen |
| **CORS** | Mecanismo que permite al servidor autorizar otros orígenes |
| **CSRF** | Ataque que aprovecha el envío automático de cookies |
| **CVE** | Identificador estándar de una vulnerabilidad |
| **CVSS** | Puntaje de 0 a 10 de la gravedad de una vulnerabilidad |
| **Distroless** | Imagen sin sistema operativo completo: sin shell ni apt |
| **Dockerfile** | Archivo con las instrucciones para construir una imagen |
| **Entidad** | Clase Java que representa una tabla de la base |
| **GHSA** | Aviso de seguridad de GitHub (otro formato de CVE) |
| **HikariCP** | Pool de conexiones a la base de datos |
| **Imagen** | Paquete inmutable con la aplicación y todo lo que necesita |
| **Ingress** | "Puerta de entrada" que expone la aplicación a internet |
| **JAR** | Archivo Java empaquetado y ejecutable |
| **JPA** | Especificación de cómo mapear objetos Java a tablas |
| **Hibernate** | Implementación concreta de JPA |
| **JWT** | Token firmado que lleva los datos del usuario |
| **KQL** | Lenguaje de consulta de Log Analytics |
| **Log Analytics** | Servicio de Azure que centraliza y consulta logs |
| **Micrometer** | Librería que expone métricas de la aplicación |
| **Mínimo privilegio** | Dar a cada identidad solo los permisos que necesita |
| **ORM** | Técnica que traduce objetos a tablas y viceversa |
| **OWASP** | Fundación que publica guías de seguridad |
| **PaaS** | Plataforma como servicio: solo subís tu aplicación |
| **Pool** | Conjunto de conexiones reutilizables |
| **Readiness** | Sonda que indica si la aplicación puede recibir tráfico |
| **Resource Group** | Carpeta lógica que agrupa recursos en Azure |
| **Revisión** | Cada versión desplegada de una Container App |
| **Runner** | Máquina descartable donde GitHub ejecuta el pipeline |
| **SBOM** | Inventario de todos los componentes de un software |
| **SHA** | Identificador único de un commit en git |
| **SemVer** | Versionado MAYOR.MENOR.PARCHE |
| **Service Principal** | Identidad para programas, con permisos acotados |
| **Singleton** | Una sola instancia compartida |
| **Suscripción** | El "contrato" de Azure; ámbito de facturación |
| **Tag** | Etiqueta que identifica una versión de una imagen |
| **Transacción** | Grupo de operaciones que se hacen todas o ninguna |
| **Workflow** | El archivo que define un proceso automático en GitHub |
