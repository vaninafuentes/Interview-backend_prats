# Cruce con la clase del profesor

Este documento cruza **lo que el profesor dijo en clase, archivo por archivo**, con lo que
realmente tiene el proyecto, y responde **las preguntas que va a hacer en la defensa**.

Sirve para dos cosas:

1. Saber qué está y qué no está, sin suposiciones.
2. Tener la respuesta lista para cada pregunta, incluidas las que tienen una respuesta distinta
   a la que el profesor espera.

---

## Índice

- [Parte 1 — Cruce archivo por archivo](#parte-1--cruce-archivo-por-archivo)
- [Parte 2 — Cinco cosas que el profesor dijo que no son exactas](#parte-2--cinco-cosas-que-el-profesor-dijo-que-no-son-exactas)
- [Parte 3 — Los tres huecos reales del proyecto](#parte-3--los-tres-huecos-reales-del-proyecto)
- [Parte 4 — Las respuestas a sus preguntas](#parte-4--las-respuestas-a-sus-preguntas)
- [Parte 5 — El guion de la demo](#parte-5--el-guion-de-la-demo)

---

# Parte 1 — Cruce archivo por archivo

## 1.1 Paquete `config`

| Lo que el profesor dice | ¿Está? | Dónde |
| :--- | :---: | :--- |
| El decorador `@Configuration` | ✅ | [SecurityConfig.java](../src/main/java/com/example/interviewback/config/SecurityConfig.java), [CorsConfig.java](../src/main/java/com/example/interviewback/config/CorsConfig.java), [AppConfig.java](../src/main/java/com/example/interviewback/config/AppConfig.java) |
| `@Autowired` inyecta dependencias (el "D" de SOLID) | ✅ mejor | Usamos **inyección por constructor**, que es la forma recomendada. Ver [2.1](#21-autowired-vs-constructor) |
| `@Bean` = "los granitos de café", un objeto global que se puede llamar desde cualquier lado | ✅ | 6 beans: `restTemplate`, `userDetailsService`, `authenticationProvider`, `authenticationManager`, `securityFilterChain`, `passwordEncoder` |
| Un bean para el cifrado de la contraseña | ✅ | `SecurityConfig.passwordEncoder()` → `BCryptPasswordEncoder` |
| El archivo de seguridad | ✅ | `SecurityConfig` |
| `UserService` donde el usuario se autentica por primera vez | ✅ | `SecurityConfig.userDetailsService()` — implementado como *bean* en vez de clase aparte |
| `UserService` con un método "buscar por username" | ✅ | [UserRepository.java](../src/main/java/com/example/interviewback/repository/UserRepository.java) → `findByEmail(<email>)` |
| El decorador `@Service` | ✅ | `AuthService`, `QuestionService`, `InterviewService`, `GroqService` |
| "Un service no tiene atributos o propiedades" | ⚠️ | Tienen campos `private final` con los repositorios, que es **cómo se pide** la dependencia. Lo que no tienen es **estado**. Ver [2.2](#22-un-service-no-tiene-propiedades) |
| El repositorio que va a buscar a la base | ✅ | `UserRepository`, `QuestionRepository`, `InterviewSessionRepository` |
| "Una interfaz de Spring Boot para que funcione" | ✅ | `interface UserRepository extends JpaRepository<User, Long>` — Spring **genera la implementación**, nosotros solo declaramos el método |
| La autenticación es previa a generar el JWT, no tiene sentido tenerlas separadas | ✅ | `AuthService.login()` verifica credenciales **y** genera el token, en el mismo método |
| Marshmallow / MapStruct: convertir objeto ↔ JSON | ✅ con otra pieza | Usamos **Jackson**, que viene dentro de `spring-boot-starter-web`. Ver [2.3](#23-mapstruct-no-hace-falta) |
| Un decorador que guarde un caché con los datos del usuario en JSON | ❌ | No hay `@Cacheable`. Ver [3.2](#32-no-hay-caché-de-usuario) |
| `@Transactional` con `readOnly = true` para eficiencia | ✅ | `AuthService.login()`, `QuestionService`, `InterviewService.getUserHistory/getUserStats` |
| "`readOnly` ayuda a nivel de seguridad: si alguien hace una inyección SQL salta por ahí" | ❌ | **Es incorrecto.** Ver [2.4](#24-readonly-no-protege-contra-sql-injection) |
| El decorador `@Override` | ✅ | `CorsConfig.addCorsMappings()`, `JwtAuthFilter.doFilterInternal()` |
| El decorador `@Entity` | ✅ | `User`, `Question`, `InterviewSession` |
| Con MySQL, el `@Id` autoincremental | ✅ | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| El decorador `@Column` | ✅ | `@Column(nullable = false)`, `@Column(nullable = false, unique = true)` |
| `UserDetails` para que el framework maneje la seguridad | ✅ vía adaptador | Usamos `User.withUsername(...)` en vez de que la entidad implemente la interfaz. Ver [2.5](#25-userdetails-como-adaptador) |
| El ORM es Hibernate; arriba está JPA / Spring Data JPA | ✅ | `spring-boot-starter-data-jpa` → Hibernate 6.6.53 |
| `ddl-auto` con `create` / `update` / `drop` desde las properties | ✅ | `spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}` |
| Solo hay usuario, sin rol | ✅ | No existe entidad `Role` ni tabla de roles |
| El bean que ejecuta la clave y genera el JWT | ✅ | [JwtUtil.java](../src/main/java/com/example/interviewback/security/JwtUtil.java) (`@Component`) |
| La parte de seguridad que dice dónde se puede entrar y dónde no: lo público y lo privado | ✅ | `SecurityConfig.securityFilterChain()` |
| CORS: a partir de cierto path necesitás JWT, el resto es público | ✅ | `.requestMatchers("/api/auth/register", "/api/auth/login").permitAll().anyRequest().authenticated()` |
| CORS: desde dónde se puede ingresar, qué métodos HTTP, cómo viene el encabezado | ✅ | [CorsConfig.java](../src/main/java/com/example/interviewback/config/CorsConfig.java) → `allowedOrigins`, `allowedMethods("GET","POST","PUT","DELETE","OPTIONS")`, `allowedHeaders("*")` |
| "Hay que agregarlo para que no rebote el método de autenticación" | ✅ | Es el *preflight*: `.cors(cors -> {})` + `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()`. Ver [4.6](#p6-cors-y-el-preflight) |
| La declaración de cómo se va a autenticar el usuario | ✅ | `DaoAuthenticationProvider` — verifica contra una base, **no importa cuál** |
| El proveedor de autenticación se le pasa a la config | ✅ | `.authenticationProvider(authenticationProvider(userDetailsService()))` |

## 1.2 La parte del servidor

| Lo que el profesor dice | ¿Está? | Dónde |
| :--- | :---: | :--- |
| El filtro por donde pasa la aplicación para ver si tiene el token o no | ✅ | [JwtAuthFilter.java](../src/main/java/com/example/interviewback/security/JwtAuthFilter.java) |
| Si no tiene token, lo deja pasar o no según la ruta | ✅ | El filtro **no rechaza**: deja pasar sin autenticar y `SecurityConfig` decide. Ver [2.6](#26-el-filtro-no-rechaza) |
| Si lo tiene, lo valida: si expiró o no, si puede o no | ✅ | `jwtUtil.isTokenValid(token)` |
| "Si el IDE está bien configurado nos agrega que los datos no pueden ser nulos" | ✅ | `@NotBlank`, `@Email`, `@Size(min=6)` en [RegisterRequest.java](../src/main/java/com/example/interviewback/dto/RegisterRequest.java) + `@Valid` en el controller |
| Cómo tenemos el usuario a través del JWT | ✅ | El principal es un `record AuthenticatedUser(id, name, email)` |
| "Como no tenemos roles, el JWT no lleva info sensible: como máximo el username" | ⚠️ | Llevamos `sub`(email), `name`, `id`. Ver [2.7](#27-qué-lleva-el-jwt) |
| `@Component` es la base de un service; `@Service` y `@Bean` heredan de `@Component` | ✅ | `JwtUtil` es `@Component`; `@Service` es una especialización de `@Component` |
| Las características del token | ✅ | Ver [4.13](#p13-qué-lleva-el-token-por-dentro) |
| La librería de JWT tiene que estar en el pom o su equivalente en Gradle | ✅ | `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` — versión 0.12.6 |
| Toma los valores de las variables de entorno `app.jwt...` | ✅ | `@Value("${jwt.secret}")` y `@Value("${jwt.expiration-ms}")` |
| "¿Lo hiciste con que recuerde el usuario?" (30 días / 7 días) | ❌ | No hay *remember me*. Ver [3.3](#33-no-hay-remember-me-ni-refresh-token) |
| Podemos tener una opción de renovar el JWT si está autenticado | ❌ | No hay *refresh token*. Misma respuesta |
| Un filtro que define si hay que encriptar, por cada petición | ✅ | `OncePerRequestFilter` — se ejecuta **una vez por petición** |
| Si programamos bien, tenemos un *token provider* y un *mapper* | ✅ | `JwtUtil` (provider) + `AuthenticatedUser` (mapper del token al objeto) |
| El controller llama al service, que no tiene un `new` porque los services ya vienen inyectados | ✅ | Ninguna clase del proyecto usa `new` para obtener un service |
| Vuelve del login con el JWT y nada más | ⚠️ | Devuelve token + `id` + `name` + `email`. Ver [4.12](#p12-qué-devuelve-el-login) |
| ¿Dónde se usa ese JWT? En la cabecera de los endpoints protegidos | ✅ | `Authorization: Bearer <token>` |
| ¿Tenemos un endpoint para validar el token? | ✅ | `GET /api/auth/me` → **200** si es válido, **403** si no. Ver [4.14](#p14-cómo-valido-un-token) |

## 1.3 HikariCP

| Lo que el profesor dice | ¿Está? | Dónde |
| :--- | :---: | :--- |
| "¿Activamos Hikari?" | ✅ | Viene activado por defecto en Spring Boot; lo configuramos explícitamente |
| Abre un máximo y mantiene un tiempo de vida | ✅ | `maximum-pool-size=10`, `min-idle=2`, `max-lifetime=20 min`, `idle-timeout=5 min` |
| Verifica si existe conexión con el servidor de la base | ✅ | Es el *health indicator* `db` de `/actuator/health` |
| Si no verifica, salta un error | ✅ | `connection-timeout=20s` → error `SQLTransientConnectionException` |
| Está relacionado con la parte de *health* de la aplicación | ✅ | `/actuator/health` con token muestra el componente `db` |
| "Si estamos con MySQL es de una manera" | ✅ | Driver `com.mysql.cj.jdbc.Driver` (mysql-connector-j). Con PostgreSQL sería otro driver y otra URL |

## 1.4 Build: `pom` / Gradle / Dockerfile

| Lo que el profesor dice | ¿Está? | Dónde |
| :--- | :---: | :--- |
| La versión de Java es 21 | ✅ | Toolchain 21 + `options.release.set(21)` |
| "Debería estar en el Dockerfile para llamarlo" | ✅ | [Dockerfile](../Dockerfile) etapa 1 |
| El Dockerfile debe en algún momento descargar todas las dependencias | ✅ | `RUN ./gradlew dependencies --no-daemon` — en su propia capa, para que quede cacheada |
| `dependencies` / `resources` / `packages` genera el `.jar` y eso se copia | ✅ | `RUN ./gradlew bootJar -x test` → `COPY --from=builder /workspace/build/libs/*.jar app.jar` |
| La siguiente imagen | ✅ | Etapa 2: `gcr.io/distroless/java21-debian12:nonroot` |
| La versión del runtime tiene que ser mayor o igual a la del proyecto | ✅ | Bytecode 21 + runtime 21. El host local tiene Java 26, pero `options.release.set(21)` fuerza bytecode 21 |
| `.m2` (el repositorio local de Maven) | ⚠️ otro | El equivalente en Gradle es `~/.gradle/caches/`. En el CI lo cachea `actions/setup-java` |
| Algo para borrar la carpeta compilada | ✅ | Maven borra `target/`; Gradle borra `build/` con `./gradlew clean` |
| "¿El CI mío hace los tests? Si los tenés, deberías sacarlos porque no va a funcionar: no tenés Maven" | ✅ **sí funciona** | El nuestro **sí corre los tests y pasan**, porque usamos el wrapper `gradlew` y el job levanta un MySQL de servicio. Ver [4.16](#p16-el-profesor-dice-que-saquemos-los-tests-del-ci) |
| "Antes de hacer push de mi aplicación he hecho los tests" | ✅ | Paso *Compilar y ejecutar los tests* en `.github/workflows/ci.yml` |
| Copia el `.jar` que está en `target` a una imagen de Java o Debian, sin bash ni intérprete | ✅ | Es **exactamente** nuestro distroless: 0 intérpretes, 0 gestor de paquetes |

---

# Parte 2 — Cinco cosas que el profesor dijo que no son exactas

Si repetís estas cinco tal cual, podés perder puntos en vez de ganarlos. Son imprecisiones de
una clase hablada, no errores de concepto, pero conviene tener la versión correcta.

## 2.1 `@Autowired` vs constructor

**Lo que dijo:** que `@Autowired` es viejo y que inyecta dependencias (el "D" de SOLID).

**La parte correcta:** sí, la inyección de dependencias es el "D" (Dependency Inversion) y
`@Autowired` sobre un **campo** efectivamente está desaconsejado.

**Lo que hacemos nosotros:** **inyección por constructor**, que es la forma recomendada.

```java
// Lo que NO hay que hacer (campo + @Autowired):
@Autowired
private AuthService authService;

// Lo que hacemos:
private final AuthService authService;

public AuthController(AuthService authService) {
    this.authService = authService;
}
```

**Por qué el constructor es mejor** (dos razones, la segunda es la que convence):

1. El campo puede ser `final` → nadie lo cambia después de crear el objeto.
2. **Si falta una dependencia, la aplicación no arranca.** Con `@Autowired` en un campo, el
   objeto se crea con el campo en `null` y explota la primera vez que lo usás, en producción y
   con un usuario esperando. Con el constructor, el error aparece al arrancar.

Además, con un solo constructor **no hace falta escribir `@Autowired`**: Spring lo detecta solo.

## 2.2 "Un service no tiene propiedades"

**Lo que dijo:** un service no tiene atributos ni propiedades.

**La versión correcta:** lo que un service no tiene es **estado** (datos que cambian y que
persisten entre llamadas). Nuestros services sí tienen campos, y tienen que tenerlos:

```java
@Service
public class AuthService {
    private final UserRepository userRepository;      // ← estos campos son
    private final PasswordEncoder passwordEncoder;    //   las dependencias
    private final JwtUtil jwtUtil;                    //   que pide, y no cambian
}
```

Son `final`, se asignan una vez, y no guardan nada de ninguna petición. Un service con estado
sería un problema de verdad: como Spring lo crea **una sola vez** (singleton), ese estado lo
compartirían todos los usuarios a la vez.

**La respuesta que conviene dar:** *"Los services no guardan estado: sus campos son las
dependencias, y son `final`. Como el bean es singleton, si guardaran estado de una petición lo
compartirían todos los usuarios."*

## 2.3 MapStruct no hace falta

**Lo que dijo:** en Flask vimos Marshmallow y MapStruct, que convierte un objeto a JSON y
viceversa.

**La aclaración:** MapStruct no convierte a JSON. **Jackson** es el que convierte objeto ↔ JSON,
y viene incluido en `spring-boot-starter-web` sin agregar nada. MapStruct sirve para otra cosa:
copiar de una clase a otra (por ejemplo, de `RegisterRequest` a `User`) generando el código.

**Nuestra respuesta:** *"La conversión a JSON la hace Jackson, que viene con el starter web. No
usamos MapStruct porque tenemos 12 DTOs chicos: un `new User(...)` explícito se lee mejor que
una interfaz generada, y no agrega una dependencia más al escaneo de vulnerabilidades."*

## 2.4 `readOnly` NO protege contra SQL injection

**Lo que dijo:** que `readOnly = true` ayuda a la seguridad y que "si alguien hace una inyección
SQL salta por ahí".

**Esto es incorrecto, y conviene decirlo con seguridad, no repetirlo.**

`readOnly = true` es **una pista de optimización**, no un control de seguridad:

- Hibernate deja de hacer *dirty checking* (no revisa si los objetos cambiaron antes de
  cerrar la transacción) → menos trabajo.
- Muchas bases lo usan para mandar la consulta a una réplica de lectura.
- En algunas bases, escribir dentro de una transacción de solo lectura falla.

**Lo que realmente protege contra inyección SQL es el uso de consultas parametrizadas.** En
nuestro caso, Spring Data JPA genera **todas** las consultas con parámetros enlazados
(`WHERE email = ?`), nunca concatenando texto. Por eso no hay superficie para inyectar.

**La respuesta que conviene dar:** *"`readOnly` es una optimización, no una defensa. Lo que nos
protege de la inyección SQL es que Spring Data genera consultas parametrizadas: el valor viaja
como parámetro enlazado, nunca como parte del texto de la consulta."*

**Dato extra que suma:** con MySQL, `readOnly=true` puede además provocar que la conexión no se
use para escritura. En `/api/interview/evaluate` **no** pusimos `@Transactional` a propósito: la
llamada a la API externa de IA mantendría una conexión del pool ocupada durante segundos,
esperando a un servicio de terceros.

## 2.5 `UserDetails` como adaptador

**Lo que dijo:** que si querés usar las características de Spring Boot, la entidad `User` debería
manejar la seguridad.

**Lo que hacemos:** usamos el `User` estándar de Spring Security, construido con un *builder*,
**en lugar de** que nuestra entidad implemente la interfaz `UserDetails`.

```java
// En SecurityConfig.userDetailsService():
return email -> userRepository.findByEmail(email)
        .map(user -> User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Roles.USER)
                .build())
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
```

**Por qué está bien así** (y es el patrón *adaptador* que el propio profesor aprobó):

- La entidad `User` queda como **modelo de datos** y no arrastra métodos de seguridad
  (`isAccountNonExpired`, `isEnabled`…) que no usamos y devolverían `true` siempre.
- La traducción entre "cómo guardamos el usuario" y "cómo lo entiende Spring Security" queda en
  **un solo lugar**, no mezclada en la entidad.
- Si mañana cambia el modelo de datos, la capa de seguridad no se toca.

**Nota de estilo:** para no repetir el nombre `User` (que existe en nuestro modelo **y** en Spring
Security), en `SecurityConfig` usamos el nombre completo de la clase de Spring. Es el único lugar
del proyecto donde pasa, y es a propósito: aclarar cuál de los dos `User` es.

## 2.6 El filtro no rechaza

**Lo que dijo:** el filtro mira si tiene el token, y si no lo tiene no lo deja pasar.

**Lo que hacemos:** el filtro **no rechaza nada**. Si no hay token, o si el token es inválido o
vencido, simplemente **deja la petición sin autenticar** y la pasa al siguiente eslabón. Es
`SecurityConfig` el que decide, según las reglas de acceso.

**Por qué está hecho así:** porque la autorización queda definida **en un solo lugar**. Si el
filtro también rechazara, tendríamos dos lugares decidiendo quién entra, y con el tiempo se
desincronizan. Hay un caso donde esto se ve claro: un token vencido sobre `/api/auth/login`
debe **seguir funcionando** (si no, no te podrías volver a loguear nunca).

## 2.7 Qué lleva el JWT

**Lo que dijo:** como no hay roles, el JWT no lleva información sensible; como máximo el username.

**Lo que llevamos:**

| Claim | Valor | ¿Sensible? |
| :--- | :--- | :--- |
| `sub` | el email | Es el "username" de nuestra app. Identifica, no autoriza |
| `name` | el nombre | No |
| `id` | el ID en la base | No |
| `iat` | cuándo se emitió | No |
| `exp` | cuándo vence | No |

**Lo que NO llevamos, y es lo importante:** contraseña (ni su hash), roles de administración, ni
ningún dato de otro usuario.

**La respuesta que conviene dar:** *"El payload no está cifrado, solo codificado en Base64:
cualquiera puede leerlo. Por eso no hay ningún dato que en sí mismo dé acceso —la contraseña no
va— y lo que impide modificarlo es la firma. Si alguien cambia el payload, deja de coincidir con
la firma y el token se rechaza."*

---

# Parte 3 — Los tres huecos reales del proyecto

Estos son los puntos donde el profesor puede encontrar algo. Uno ya está corregido y verificado;
los otros dos son decisiones de diseño con respuesta preparada.

## 3.1 `JWT_SECRET` tenía un valor por defecto en el repositorio — ✅ CORREGIDO

**✅ Estado: cerrado.** Se quitó el valor por defecto. Esta sección queda como registro del
problema y de la verificación, porque es el punto que el profesor dijo que iba a revisar
(*"quiero ver si realmente no hardcodea la parte de JSON Web Token"*).

**El problema que había:** no había ningún token hardcodeado, pero la **clave para firmar** tenía
valor por defecto en tres archivos versionados. Nunca se usaba en producción, pero tenía dos
defectos reales: a la vista era una clave en el repositorio, y si alguien desplegaba sin
configurar la variable, la aplicación **arrancaba igual** firmando con una clave pública.

**Lo que se hizo:**

| Archivo | Antes | Ahora |
| :--- | :--- | :--- |
| `application.properties` | `${JWT_SECRET:<clave por defecto>}` | `${JWT_SECRET}` — sin valor por defecto |
| `docker-compose.yml` | `${JWT_SECRET:-<clave por defecto>}` | `${JWT_SECRET:?Falta JWT_SECRET...}` — falla con mensaje claro |
| `JwtUtil` | sin validación | `@PostConstruct` que rechaza claves de menos de 32 bytes al arrancar |
| `.env.example` | etiqueta "HS256" incorrecta | etiqueta corregida + cómo generar la clave |

**Verificado con ejecución real** (no es una suposición):

| Caso | Resultado |
| :--- | :--- |
| Sin `JWT_SECRET` | ❌ No arranca → `Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}" <-- "${jwt.secret}"` |
| Con clave de 11 bytes | ❌ No arranca → `JWT_SECRET es demasiado corta: se requieren al menos 32 bytes y tiene 11` |
| Con clave válida de 48 bytes | ✅ Arranca → `Started InterviewBackApplication in 6.029 seconds` |
| `docker compose config` sin la variable | ❌ Falla → `required variable JWT_SECRET is missing a value: Falta JWT_SECRET...` |
| `docker compose config` con la variable | ✅ Configuración válida |

**Quién provee la variable ahora:**

| Entorno | Quién la provee |
| :--- | :--- |
| Pipeline (tests) | `ci.yml` — clave de 55 bytes solo para los tests |
| Docker Compose | El `.env` local (plantilla en `.env.example`) |
| Azure | Secreto del Container App |
| `./gradlew bootRun` | Hay que exportarla antes — **a propósito** |

**El único caso que requiere un paso manual es el desarrollo local**, y es intencional: es
preferible que falle al arrancar a que arranque con una clave conocida. Cómo se configura está
en el [README](../README.md#variable-jwt_secret).

## 3.2 No hay caché de usuario

**Lo que el profesor mencionó:** un decorador que guarde un caché con los datos del usuario en
JSON.

**No lo tenemos.** Y conviene tener clara la respuesta, porque hay una confusión frecuente:

**Un JWT no es un caché.** Son cosas distintas:

- Un **caché** guarda una copia de datos para **evitar volver a calcularlos o consultarlos**. Vive
  en memoria (o en Redis), tiene expiración propia y lo administra el servidor.
- Un **JWT** es un **token firmado** que *transporta* identidad y claims. No guarda nada ni evita
  ninguna consulta: solo afirma quién es el usuario, y la firma garantiza que nadie lo modificó.

Decir "el token es el caché" sería incorrecto: el token **no reemplaza** a un caché de usuarios
como Redis. Lo que hace el token es reemplazar el **almacén de sesiones**, que es otra cosa.

**Por qué no necesitamos un caché:**

- La única consulta que se repite es `findByEmail`, sobre una columna **única e indexada**, y
  **una sola vez por petición**. Es una lectura por índice: barata.
- No hay consultas caras ni encadenadas que justifiquen guardar el resultado.
- Un caché agregaría un problema nuevo: un usuario desactivado seguiría siendo válido hasta que
  venza la entrada, en vez de dejar de serlo de inmediato.

**Cuándo sí haría falta:**

- Si el usuario se armara con **varias consultas** caras por petición.
- Si hubiera **varios servicios** que necesitaran compartir el estado de sesión.
- Si el tráfico fuera alto y esa consulta apareciera en el perfil como cuello de botella.

En ese caso la implementación sería `@EnableCaching` + `@Cacheable("users")` para un caché local
en memoria, o **Redis** si hiciera falta compartirlo entre réplicas.

**Redis, concretamente, para qué sería:** para estado **compartido entre varias réplicas**. Hoy la
aplicación no tiene estado que compartir —el token viaja con cada petición y los datos están en
MySQL—, así que agregarlo sería un servicio más para pagar y mantener, sin resolver ningún
problema que exista.

**La respuesta para el profesor:** *"No tenemos caché de usuarios. La consulta que se repite es
una lectura por índice sobre una columna única, una vez por petición. Un caché agregaría el
problema de que un usuario desactivado siga siendo válido hasta que venza la entrada. Si
hicieran falta varias réplicas con estado compartido, ahí sí evaluaría Redis; hoy no hay estado
compartido que guardar."*

## 3.3 No hay "remember me" ni refresh token

**Lo que el profesor preguntó:** si lo hicimos con que recuerde el usuario (30 días / 7 días), y
si hay opción de renovar el token.

**No lo tenemos. La respuesta:**

- **Recordar al usuario es del cliente, no del servidor.** El token dura **7 días**
  (`jwt.expiration-ms=604800000`). Quien decide si lo guarda es la app: si lo guarda en
  almacenamiento persistente, el usuario sigue logueado; si lo guarda en memoria, se desloguea al
  cerrar. El backend ya emite un token de larga duración; no hay nada más que hacer del lado del
  servidor.
- **"Remember me" existe en Spring Security para sesiones con formulario**, que es un modelo
  distinto: cookie de sesión + cookie recordatoria. Nuestro modelo es sin estado y con la
  cabecera `Authorization`, así que esa función no aplica.
- **Un refresh token** tendría sentido si el token durara minutos (para poder revocarlo). Con 7
  días de validez **agregaría un endpoint más, una tabla más y una rotación que hay que
  mantener a mano**, a cambio de nada que el proyecto necesite.

---

# Parte 4 — Las respuestas a sus preguntas

## Bloque A — El pipeline

### P1. ¿Está la parte de integración y despliegue continuo?

Sí. Un workflow, dos jobs:

```
Job 1 — build-scan-push   (SIEMPRE se ejecuta)  → Integración Continua
   1. Descarga el código
   2. Instala Java 21 y cachea Gradle
   3. Compila y ejecuta los tests contra un MySQL efímero
   4. Construye la imagen Docker con etiquetas versionadas
   5. La escanea con Grype y publica el informe como artefacto
   6. Se autentica en Azure con el Service Principal
   7. Publica la imagen en el ACR

Job 2 — deploy            (SOLO si hay tag vX.Y.Z o se marcó la casilla)  → Despliegue Continuo
   1. Login en Azure
   2. Determina qué versión desplegar
   3. Verifica que esa etiqueta exista en el ACR
   4. az containerapp update
   5. Espera y verifica el health check
```

### P2. ¿Es el mismo pipeline para cualquier aplicación?

Casi. Lo genérico (y esto es lo que el profesor remarcó) es la **estructura**: checkout → setup
del lenguaje → tests → build → scan → push → deploy → health check. Lo específico de cada
proyecto es solo:

- El lenguaje y su gestor (`actions/setup-java` + `gradlew`; en otro proyecto sería
  `actions/setup-node` + `npm`).
- El servicio de base de datos que necesitan los tests (el nuestro levanta MySQL).

**El resto del YAML no cambia**: los pasos de Docker, Grype, Azure y el despliegue son idénticos
para cualquier aplicación que se empaquete en una imagen.

### P3. El profesor pide hacer un cambio, subirlo y ver el "caminito de los pasos"

**Esto es lo más probable que te haga hacer en vivo.** Tenés el guion completo en la
[Parte 5](#parte-5--el-guion-de-la-demo).

**Lo importante que tenés que saber de antemano:** un push normal a `main` **NO despliega**. Solo
construye y publica. Para que despliegue hacen falta una de dos:

- Pushear una **etiqueta** `vX.Y.Z`, o
- Ejecutar **Run workflow** marcando la casilla `deploy`.

**Decisión pendiente:** si el profesor espera que *"cambio → push → se despliega solo"*, hay dos
caminos: (a) explicar el diseño —que es deliberado y es el que justifica el factor V de los 12
factores— y demostrarlo con un tag; o (b) hacer que `main` también despliegue. **Recomendación:**
(a), y tener el tag a mano. Publicar sin liberar es una decisión de ingeniería defendible; un
push que libera solo es lo que la consigna del proyecto original hacía mal.

### P4. "Matame la aplicación": ¿cómo sé si está funcionando?

**El profesor lo dijo bien y hay que responderlo igual:** que el repositorio muestre el paso en
verde **no alcanza**. Dice que se creó la instancia, no que la aplicación funcione. Puede crear
la instancia perfectamente y no funcionar porque no le pasaste las variables de entorno.

**Cuatro verificaciones, de más rápida a más profunda:**

| # | Verificación | Comando | Qué te dice |
| :---: | :--- | :--- | :--- |
| 1 | El pipeline, al final | job `deploy`, paso 5 | El propio pipeline no declara éxito hasta que `/actuator/health` devuelve 200 |
| 2 | El health público | `curl https://<fqdn>/actuator/health` | `{"status":"UP"}` o `503` con el motivo |
| 3 | El health con detalle | `bash scripts/admin.sh estado` | Estado componente por componente, incluida la base |
| 4 | Azure Monitor y Log Analytics | Portal → Container App → Metrics / Logs | Réplicas, peticiones, reinicios y los logs del arranque |

**La prueba de que esto es real, y no teoría:** durante el desarrollo la aplicación devolvía
**503** en Azure. El repositorio mostraba el pipeline en verde y la instancia creada. La causa
estaba en los logs de Log Analytics: `Connection refused` → no podía crear el `EntityManagerFactory`
→ Tomcat no arrancaba. Faltaban las variables de entorno de la base. **Ese es exactamente el
escenario que el profesor describe**, y lo vivimos y lo diagnosticamos.

### P5. ¿Con qué punto de los 12 factores se relacionan las métricas y el monitoreo?

**Factor XI — Logs: tratá los logs como un flujo de eventos.**

El factor dice que la aplicación **nunca** debe administrar sus propios archivos de log: emite
todo por la salida estándar y **el entorno decide qué hacer con eso**.

Nuestro caso:

- La aplicación escribe a la salida estándar (consola). No abre ni rota archivos.
- Azure Container Apps captura esa salida y la manda a **Log Analytics**, donde queda consultable
  con KQL y con retención configurable. Eso es "el historial" al que se refiere el profesor.
- Las **métricas** son la parte numérica, y llegan por un camino distinto: Micrometer las expone
  en `/actuator/metrics` y Azure las recolecta (además de las que la plataforma mide sola:
  CPU, memoria, réplicas, reinicios).

**Factores secundarios que también se pueden mencionar:**

- **Factor IV — Backing services:** la base es un recurso adjunto identificado por configuración,
  no por código. Cambiar MySQL local por el de Azure solo cambió una variable de entorno.
- **Factor IX — Disposability:** la aplicación arranca rápido y se apaga limpio, lo que permite
  escalar a cero y crear réplicas cuando hace falta.

### P6. CORS y el preflight

**Lo que preguntó:** que hay que agregar la configuración "para que no rebote el método de
autenticación".

**Qué es:** cuando un navegador va a hacer una petición que considera "no simple" (con la
cabecera `Authorization`, o con `Content-Type: application/json`), **primero manda sola** una
petición `OPTIONS` al mismo endpoint para preguntar "¿me dejás?". Eso es el *preflight*. Si esa
`OPTIONS` falla, la petición real nunca se hace.

**Por qué nos importa:** todos nuestros endpoints protegidos mandan `Authorization`, así que
**todas** disparan un `OPTIONS` previo. Sin el manejo explícito, el navegador nunca llegaría a
mandar el `POST /api/auth/login`.

**Las dos piezas que lo resuelven:**

```java
// 1. Habilitar CORS en la cadena de filtros
.cors(cors -> {})

// 2. Dejar pasar el OPTIONS SIN autenticación
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
```

**Por qué el paso 2 es imprescindible:** el `OPTIONS` **no lleva** la cabecera `Authorization` —
el navegador todavía no sabe si puede. Si exigiera autenticación, rebotaría con 401 y el
navegador abortaría. Hay que dejarlo pasar para que la respuesta CORS llegue con los encabezados
`Access-Control-Allow-*` y el navegador se convenza.

### P7. El profesor va a probar la aplicación: ¿cómo se autentica?

**Respuesta corta: con Postman, pegándole a la URL del Container App.**

Ya está armada la colección de Postman con **13 peticiones en 4 carpetas** y scripts de test que
guardan el token automáticamente entre peticiones:

📄 `postman/Interview-Backend.postman_collection.json`

**El flujo, con los endpoints exactos:**

```bash
FQDN=interview-backend.happybay-cc24db11.brazilsouth.azurecontainerapps.io

# 1. Registrarse (público) → 201
curl -X POST "https://${FQDN}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"name":"Vanina","email":"vanina@test.com","password":"secreto123"}'

# 2. Loguearse (público) → 200, devuelve el token
curl -X POST "https://${FQDN}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"vanina@test.com","password":"secreto123"}'

# 3. Usar el token en un endpoint protegido → 200
curl "https://${FQDN}/api/auth/me" -H "Authorization: Bearer $TOKEN"
```

### P8. ¿Cómo obtengo el nombre de dónde se está ejecutando la aplicación?

Ese nombre es el **FQDN** (nombre de dominio completo) y se obtiene de la propiedad
`ingress.fqdn` del Container App:

```bash
# Desde el terminal
az containerapp show \
  --name interview-backend \
  --resource-group rg-interview-app \
  --query properties.configuration.ingress.fqdn -o tsv

# O con el script, que ya lo hace
bash scripts/admin.sh url
```

**Desde el portal:** Container Apps → `interview-backend` → **Overview** → el campo
**Application Url**.

**Por qué es así:** el FQDN no se elige, lo asigna Azure, y lleva un sufijo aleatorio
(`happybay-cc24db11`) porque el ambiente de Container Apps comparte el dominio
`brazilsouth.azurecontainerapps.io` entre todos los clientes de la región. Por eso **no se puede
hardcodear** y hay que consultarlo.

### P9. Cuatro preguntas de jerarquía de Azure

**"El Azure Container Registry, ¿de quién depende?"**

Del **grupo de recursos**, y el grupo de recursos de la **suscripción**. El ACR no depende del
Container App: son dos recursos **hermanos** dentro de `rg-interview-app`.

```
Suscripción  (fd36e283-…  — la de Azure for Students)
    │
    └── Grupo de recursos  rg-interview-app  (brazilsouth)
            ├── ACR                acrvaniainterview
            ├── MySQL Flexible Server   mysql-interview-backend
            ├── Container Apps Environment  env-interview-app
            │       └── Container App       interview-backend
            └── Log Analytics      workspace-rginterviewappMrgY
```

**La relación real entre el ACR y el Container App** no es de dependencia sino de **credenciales**:
el Container App guarda usuario y contraseña del registro para poder **bajar** las imágenes.

**"¿Y la red?"**

El Container Apps Environment crea su propia red virtual (una VNet gestionada por Azure) y la
mantiene dentro del grupo de recursos. Nosotros **no** creamos una VNet propia: usamos la que
viene por defecto con ingreso público. Una VNet propia se justificaría si quisiéramos que la
base **no** fuera accesible desde internet, o para conectar con una red corporativa.

**"¿Entonces cada grupo de recursos es un proyecto distinto?"**

- **Suscripción = límite de facturación y de cuotas.** Todo lo que pasa adentro se factura junto.
  Cada proyecto debería tener su propia suscripción si son clientes distintos, porque la factura
  se separa ahí.
- **Grupo de recursos = límite de ciclo de vida y de permisos.** Es la unidad que se crea y se
  **borra entera** de una sola vez, y es el alcance sobre el que se asignan roles. Varios grupos
  de recursos pueden vivir en la misma suscripción (por ejemplo, uno por ambiente: `rg-app-dev`,
  `rg-app-prod`).

**"El Service Principal, ¿para qué lo uso? ¿Dónde lo utilizamos?"**

Se usa **en un solo lugar: el pipeline**, en el paso *Login en Azure*:

```yaml
- name: Login en Azure (Service Principal)
  uses: azure/login@v3
  with:
    creds: ${{ secrets.AZURE_CREDENTIALS }}
```

Sin él, GitHub no puede publicar imágenes ni tocar el Container App. Con sus roles:

| Rol | Alcance | Para qué |
| :--- | :--- | :--- |
| `AcrPush` | solo el ACR | Subir imágenes |
| `Container Apps Contributor` | solo el Container App | Desplegar |

Nada más. No es Contributor de la suscripción: si alguien obtuviera esas credenciales, no podría
tocar la base ni otros recursos.

### P10. "Tengo que llevarlo sin vulnerabilidades o mitigadas"

**Estado actual: 186 → 115 hallazgos (−38 %). Críticas: 10 → 3.**

Las 3 críticas restantes están **documentadas y justificadas**, no ignoradas:

| CVE | Componente | Por qué queda |
| :--- | :--- | :--- |
| spring-webmvc 6.2.19 | Framework | Es la última versión de la rama 2.x. La corrección está en la 6.3+, que no es compatible con Spring Boot 3.5 |
| libc6 | Imagen base distroless | Depende de que Google reconstruya la imagen. Nosotros no controlamos ese componente |

**Grype vs Snyk vs Trivy — la comparación:**

| Herramienta | Qué escanea | Modelo | Qué la hace distinta |
| :--- | :--- | :--- | :--- |
| **Grype** | Imágenes, dependencias y paquetes del SO | Open source, sin cuenta | Se integra contra el SBOM de la imagen. Es la que usamos |
| **Snyk** | Dependencias, imagen, código (SAST) e IaC | Comercial, con capa gratuita limitada | Avisa el *exploit maturity* y abre PRs con el arreglo. La parte gratuita tiene tope de tests por mes |
| **Trivy** | Igual que Grype **más** malas configuraciones y secretos filtrados | Open source, sin cuenta | Más amplio: también detecta secretos en el repositorio |

**"Una me lo da más actualizado":** las dos bases se actualizan a diario y se complementan. La
diferencia práctica no es la frecuencia sino la **cobertura** y **cómo miden**. No hay una que
sea "más nueva" de forma consistente.

**La respuesta completa para el profesor:** *"Usamos dos herramientas que cubren cosas distintas,
no una en lugar de la otra: **Grype** sobre la imagen terminada —dependencias y sistema
operativo— y **CodeQL** sobre nuestro código, que es lo que haría Snyk Code. El mismo hallazgo
puede aparecer en las dos y no es doble conteo: una mira lo que escribimos, la otra lo que
descargamos."*

### P11. Si el profesor pide un cambio que NO debe crear la imagen

**Esta pregunta se contesta sola con el pipeline, y es donde brilla.** Hay tres caminos distintos:

| Qué querés | Qué hacés | Resultado |
| :--- | :--- | :--- |
| **Cambio sin construir nada** | Push a una **rama** (no `main`) | El workflow **no se dispara**: solo escucha `main` y tags |
| **Construir y publicar, sin desplegar** | Push a **`main`** | Imagen en el ACR con tag del SHA. La app sigue con la versión anterior |
| **Construir, publicar y desplegar** | Push de un **tag `vX.Y.Z`** | Imagen con dos tags y nueva revisión en producción |

**Decir esto textual:** *"Construir y liberar son dos decisiones separadas. El push a `main`
construye y publica pero no toca producción; la etiqueta git es la que libera. Y con Run
workflow puedo elegir a mano si despliego y cuál versión, incluso una anterior."*

### P12. ¿Qué devuelve el login?

El profesor dijo "devuelve el JWT y nada más". Nosotros devolvemos **token + id + name + email**.

**Por qué:** el cliente (la app React Native) necesita el nombre para saludar al usuario en la
pantalla principal. Si devolviéramos solo el token, tendría que hacer **una segunda petición**
a `/api/auth/me` para conseguir el mismo dato.

**Por qué no es un problema:** ninguno de los tres es sensible — son los datos del usuario que
acaba de autenticarse, no de terceros. La contraseña no se devuelve nunca, ni hasheada.

**Si pregunta "¿no era que solo el token?":** *"El front necesita el nombre para la primera
pantalla. Devolverlo junto con el token evita un viaje de red extra. Son datos del propio usuario
que se acaba de autenticar, no de terceros."*

### P13. Qué lleva el token por dentro

```
Header     { "alg": "HS384", "typ": "JWT" }
Payload    { "sub": "<email>", "name": "...", "id": 1, "iat": ..., "exp": ... }
Signature  HMAC-SHA384( base64(header) + "." + base64(payload), clave )
```

**Tres cosas para decir:**

1. **El algoritmo lo elige la librería según el largo de la clave.** Con 48 bytes usa HS384. Con
   32 usaría HS256. Y una clave de **menos** de 32 bytes la rechaza directamente, lo que evita
   firmas débiles. (En el proyecto de referencia estaba documentado como HS256 y la clave era de
   48 bytes: corregido.)
2. **El payload no está cifrado, solo en Base64.** Cualquiera puede leerlo. Lo que impide
   modificarlo es la firma: si alguien cambia un solo carácter del payload, deja de coincidir con
   la firma calculada con la clave secreta y el token se rechaza.
3. **Vence a los 7 días** (`604800000` ms), y lo valida Spring Security en cada petición.

### P14. ¿Cómo valido un token?

**`GET /api/auth/me`** es el endpoint de validación:

```bash
curl -i "https://${FQDN}/api/auth/me" -H "Authorization: Bearer $TOKEN"
```

| Respuesta | Significado |
| :---: | :--- |
| **200** | El token es válido y no venció. Devuelve `{id, name, email}` |
| **403** | El token falta, está mal firmado o venció |

**Detalle fino que conviene saber:** un token al que se le **agrega un carácter al final**
devuelve 200, no 403. No es una vulnerabilidad: el decodificador Base64 ignora el relleno
sobrante, así que los bytes decodificados son **idénticos** —es el mismo token, sin ningún
privilegio extra—. Los casos que sí importan devuelven 403 correctamente: alterar el **medio de
la firma** (403), alterar el **payload** (403), agregar **3 o más** caracteres (403).

### P15. ¿Dónde queda la imagen versionada? ¿Dónde va a parar?

**En el Azure Container Registry, `acrvaniainterview`, en el repositorio `interview-backend`.**

En el pipeline eso pasa en el paso **Publicar la imagen en el ACR**, dentro del job 1
(`build-scan-push`) — el job 2 solo la *señala*, no la sube.

Cada imagen lleva **dos etiquetas**:

| Etiqueta | Cuándo | Ejemplo | Para qué |
| :--- | :--- | :--- | :--- |
| SHA corto del commit | **Siempre** | `:6aec073` | Identifica exactamente el código que la produjo |
| Versión semántica | Solo si el push fue un tag `vX.Y.Z` | `:v1.1.0` | Marca una versión liberada |
| `:latest` | Siempre | `:latest` | Comodidad. **El despliegue nunca la usa** |

**Cómo se comprueba en vivo:**

```bash
bash scripts/admin.sh versiones
# o
az acr repository show-tags --name acrvaniainterview \
  --repository interview-backend -o tsv
```

**Desde el portal:** ACR → **Repositories** → `interview-backend` → la lista de etiquetas, con la
fecha y el digest de cada una.

**Por qué el despliegue no usa `:latest`:** porque `latest` es una etiqueta **móvil**. Si se
despliega con ella, no se sabe qué código está corriendo, y volver a una versión anterior no es
posible. Con una etiqueta fija, revertir es desplegar otra etiqueta.

### P16. El profesor dice que saquemos los tests del CI

**Cuidado: ese consejo es para quien no tiene el gestor de build.** El profesor dijo *"si lo
tenés, lo deberías sacar porque no va a funcionar, no tenés Maven"*. Es el caso de alguien que
copió un paso `mvn test` sin tener Maven.

**El nuestro sí funciona.** Razones por las que funciona:

1. **Usamos Gradle con el wrapper**, y `gradlew` está **commiteado en el repositorio**. El wrapper
   descarga la versión correcta de Gradle por su cuenta: no hace falta tener Gradle instalado en
   la máquina ni en el runner.
2. Los tests necesitan una base de datos, y el job **levanta un MySQL efímero como contenedor de
   servicio** (`services: mysql:`) con su health check, y le pasa la URL por variable de entorno.
3. En el Dockerfile, en cambio, los tests van **excluidos** (`bootJar -x test`), porque ahí no hay
   base de datos. Los tests corren en el pipeline, no en la construcción de la imagen.

**Decir esto textual:** *"Los tests no se desactivan: si fallan, el pipeline se detiene ahí y no
llega a publicar. Corren con el wrapper de Gradle contra un MySQL que el job levanta como
contenedor de servicio. En el Dockerfile van excluidos, porque ahí no hay base."*

**Aparte de la corrección técnica, hay un argumento académico:** que un pipeline **detenga el
despliegue cuando un test falla** es la definición de Integración Continua. Sacarlos lo
convertiría en un simple empaquetador.

### P17. ¿Dos entornos, una imagen de producción y otra de desarrollo?

**Tenemos dos imágenes, pero no dos ambientes distintos:**

| | Imagen de desarrollo | Imagen de producción |
| :--- | :--- | :--- |
| **Quién la construye** | Docker Compose, local | El pipeline, en GitHub |
| **Etiqueta** | `interview-backend:compose` | `acrvaniainterview/interview-backend:v1.1.0` |
| **Se publica** | No, nunca sale de tu máquina | Sí, al ACR |
| **Dockerfile** | El mismo | El mismo |
| **Base de datos** | MySQL en un contenedor, en tu máquina | MySQL Flexible Server, en Azure |

**La idea que hay detrás, y conviene decirla:** se construye **una sola vez** y se promueve la
misma imagen entre entornos, cambiando solo la configuración. Es el **factor V** de los 12
factores (*build, release, run*): la construcción es una etapa separada de la ejecución, y el
*lanzamiento* (release) es la construcción **más** la configuración. Reconstruir una imagen
distinta para producción significa que lo que probaste no es lo que desplegaste.

### P18. ¿Cómo freno la aplicación?

**✅ Ya está implementado en `scripts/admin.sh`.** El profesor ofrece un punto extra si se
demuestra *desde el terminal*.

#### La vía correcta: la API oficial de detener/iniciar

```bash
bash scripts/admin.sh app            # consultar el estado (solo lectura)
bash scripts/admin.sh app-frenar     # detener (pide confirmación)
bash scripts/admin.sh app-iniciar    # volver a levantar (pide confirmación)
```

Por dentro, el script llama a la API REST oficial con `az rest`:

```
POST .../Microsoft.App/containerApps/interview-backend/stop?api-version=2025-07-01
POST .../Microsoft.App/containerApps/interview-backend/start?api-version=2025-07-01
```

#### ⚠️ Dos cosas que NO funcionan, y conviene saber por qué

**1. `az containerapp stop` — existe en la documentación, pero no en el CLI instalado.**

El subcomando es reciente. En la versión instalada (2.89.1) **no está reconocido**:

```
$ az containerapp stop --help
ERROR: 'stop' is misspelled or not recognized by the system.
```

Por eso el script usa `az rest` contra la misma API: la operación es idéntica, solo cambia la
forma de invocarla, y así **no depende de la versión del CLI**.

**2. `--max-replicas 0` — no es válido, y además no sería equivalente.**

Dos motivos, y los dos importan:

**a) Azure rechaza el valor.** La documentación oficial fija el mínimo de `maxReplicas` en **1**:

| Límite | Valor por defecto | Mínimo | Máximo |
| :--- | :---: | :---: | :--- |
| Réplicas **mínimas** por revisión | 0 | **0** | 1.000 |
| Réplicas **máximas** por revisión | 10 | **1** | 1.000 |

**b) Aunque se aceptara, escalar a cero NO es detener.** Son cosas distintas:

| | Escalar a cero (`minReplicas: 0`) | Detener (API `stop`) |
| :--- | :--- | :--- |
| ¿Atiende una petición que llega? | ✅ **Sí**: crea una réplica y responde | ❌ No: rechaza el tráfico |
| ¿Se puede "despertar" solo? | ✅ Sí, con la primera petición | ❌ No: hay que llamar a `start` |
| ¿Para qué sirve? | Ahorro de costo con tráfico intermitente | **Acción operativa**: garantizar que no arranque |

La documentación de Microsoft lo dice explícitamente: *"Stopping an app is different. Stopping is
an explicit operational action, and it's useful when you want to guarantee the app doesn't start
replicas while you're mitigating an incident."*

**La frase para la defensa:** *"Escalar a cero es una configuración de costo: la aplicación sigue
viva y se despierta sola con la primera petición. Detener es una acción operativa: la aplicación
no arranca hasta que se la inicie explícitamente. No son lo mismo, y Azure no permite
`maxReplicas` en 0."*

#### Verificación de la implementación

| Qué se verificó | Cómo | Resultado |
| :--- | :--- | :--- |
| La URL coincide con la API oficial | Comparada contra la referencia de Microsoft Learn | ✅ Idéntica |
| `az rest` acepta las banderas usadas | `az rest -h` | ✅ `--url`, `--method` |
| Sintaxis del script | `bash -n scripts/admin.sh` | ✅ Sin errores |
| El estado correcto al detener | Referencia oficial de la API | ✅ `"runningStatus": "Stopped"` |
| Manejo de error de la ruta de lectura | Ejecutado con la sesión de Azure bloqueada | ✅ Diagnostica la causa en vez de fallar en silencio |
| El script no toca la configuración | Revisión del código: solo emite el POST de stop/start; nunca llama a `containerapp update` ni escribe secretos | ✅ Solo cambia el estado |
| **Detener y volver a iniciar de verdad** | — | ⏳ **Pendiente** — requiere sesión activa y autorización |
| **Que la configuración y los secretos se conserven** | — | ⏳ **Se confirma en esa misma prueba** |

**Los dos últimos puntos son lo único sin verificar.** El comando está implementado, su
construcción está validada contra la documentación oficial, y el script no toca ninguna
configuración —solo emite el POST de stop/start—, pero **no se ejecutó contra Azure** porque la
sesión de CLI está bloqueada (`AADSTS530035`).

**Antes del parcial hay que hacer una pasada en vivo** con la sesión activa, y en esa misma
prueba conviene capturar:

```bash
# Antes de detener: guardar la configuración para comparar
az containerapp show -n interview-backend -g rg-interview-app \
  --query "{imagen:properties.template.containers[0].image, secretos:properties.configuration.secrets[].name, variables:properties.template.containers[0].env[].name}" -o json

# Detener y volver a iniciar
bash scripts/admin.sh app-frenar
bash scripts/admin.sh app-iniciar

# Después: repetir la primera consulta y comparar que sea idéntica
```

**El detalle que suma puntos:** la base de datos **sí** tiene un stop de verdad, y es la que más
gasta:

```bash
bash scripts/admin.sh detener    # az mysql flexible-server stop
bash scripts/admin.sh encender   # az mysql flexible-server start
```

### P19. Métricas: ¿dónde pongo la parte de métricas? ¿Dónde van esos datos?

**El profesor comparó con el proyecto de Go, donde la aplicación *empujaba* los datos a Azure.**

**Acá el camino es al revés, y conviene explicarlo:** la aplicación **expone** y la plataforma
**recolecta**. No hay que mandar nada a ningún lado.

```
Aplicación (Micrometer)          Azure
  /actuator/health          ──►   Sondas de vida y preparación (liveness / readiness)
  /actuator/metrics         ──►   Azure Monitor recolecta las métricas
  salida estándar (logs)    ──►   Log Analytics → consultas KQL

Además, la plataforma mide sola (sin tocar el código):
  peticiones · réplicas · reinicios · CPU · memoria · latencia
Y métricas nativas de la JVM, porque es un Container Apps:
  heap, garbage collector, hilos
```

**Qué hay que tocar en el proyecto:** prácticamente nada, y eso es lo bueno.

1. `spring-boot-starter-actuator` en `build.gradle.kts` — ya está.
2. Tres líneas en `application.properties` — ya están.
3. Un tag para poder filtrar por aplicación: `management.metrics.tags.application=interview-backend`.

**Lo que NO hay que hacer:** instalar un agente, escribir un exportador, ni configurar un endpoint
de ingesta. La plataforma ya sabe leer `/actuator`.

**La respuesta:** *"En Go empujábamos las métricas. Acá la aplicación las expone con Micrometer
en `/actuator`, y Azure las recolecta. Lo único que tuve que agregar fueron tres líneas de
configuración y una etiqueta para identificar la aplicación. Lo demás lo toma la plataforma."*

### P20. "Quiero ver si realmente no hardcodea la parte de JWT"

**La parte de los tokens está limpia:** no hay ningún token escrito en el código. La clave y el
tiempo de vencimiento se leen con `@Value("${jwt.secret}")` y `@Value("${jwt.expiration-ms}")`,
que son referencias a la configuración, no valores.

**Y hay un punto que antes se podía marcar y ya está corregido:** la clave tenía un valor por
defecto de desarrollo escrito en `application.properties`. Se quitó, y ahora la aplicación **no
arranca** si falta o si es demasiado corta. Ver [3.1](#31-jwt_secret-tenía-un-valor-por-defecto-en-el-repositorio--corregido),
con las cinco pruebas de ejecución real.

**Cómo demostrar que está bien:** mostrar que en el Container App la clave es un **secreto**, no
una variable común:

```bash
az containerapp show -n interview-backend -g rg-interview-app \
  --query "properties.configuration.secrets[].name" -o tsv
```

### P21. ¿Dónde va el string de conexión? ¿No debería estar en `properties`?

**El profesor tiene razón en el principio, y nuestra configuración está armada exactamente para
eso:** en `application.properties` **no hay ninguna credencial real**.

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/...}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}
jwt.secret=${JWT_SECRET}
```

**La sintaxis `${VARIABLE:valor_por_defecto}` significa:** usá la variable de entorno; si no
existe, usá el valor por defecto. En producción **las variables están definidas** en el Container
App, así que los valores por defecto **nunca se usan**. Y `jwt.secret` directamente **no tiene**
valor por defecto: sin la variable, la aplicación no arranca.

**El matiz honesto, que conviene decir en vez de que te lo encuentren:** los valores por defecto
`root`/`root` son la contraseña del MySQL que levanta Docker Compose **en tu máquina**. No son
credenciales de ningún servicio real, y no dan acceso a nada fuera de tu equipo. Aun así están a
la vista, y es la única credencial que queda así —la de JWT ya se sacó
([3.1](#31-jwt_secret-tenía-un-valor-por-defecto-en-el-repositorio--corregido))—. Si el profesor
lo marca, la respuesta es que son valores de conveniencia para el entorno local y que en Azure
la contraseña es un secreto del Container App.

**La respuesta:** *"En properties no hay credenciales: hay referencias a variables de entorno con
valor por defecto solo para desarrollo local. En Azure las variables están definidas y los
valores por defecto no se usan. Podés comprobarlo con `az containerapp show`."*

### P22. ¿Dónde pongo las claves: en bóveda o en los secretos de GitHub?

**En los dos, pero en capas distintas, y cada una resuelve un problema distinto:**

| Capa | Dónde | Qué protege | Quién lo lee |
| :--- | :--- | :--- | :--- |
| **GitHub Secrets** | Settings → Secrets and variables → Actions | Que las credenciales no queden en el YAML del workflow | El pipeline, en tiempo de ejecución |
| **Container App secrets** | Configuración del Container App | Que las credenciales no se vean en el portal ni en la configuración | La aplicación, al arrancar |
| **Azure Key Vault** *(no lo usamos)* | Recurso aparte | Rotación, auditoría y control de acceso centralizado | Ambos |

**Por qué los secretos del Container App son un secreto y no una variable común:** una variable
común se ve en texto plano en la configuración del recurso y en cualquier exportación de la
plantilla ARM. Un secreto, no. Y hay una diferencia técnica concreta: los secretos de Container
Apps se pueden respaldar con **Key Vault**, y se rotan sin reconstruir la imagen.

**Cuándo se justificaría Key Vault:** con varios entornos y rotación obligatoria por política.
Para un proyecto de una sola aplicación, agrega un recurso más que mantener y una dependencia
más que se puede romper. **Mencionarlo como la evolución natural, no como una falta.**

### P23. ¿Qué más va en secretos, además de lo que teníamos en Go?

**Los secretos del repositorio (5):**

| Secreto | Qué es |
| :--- | :--- |
| `ACR_REGISTRY` | El nombre del registro (`acrvaniainterview.azurecr.io`) |
| `AZURE_CREDENTIALS` | El **JSON** completo del Service Principal |
| `AZURE_RESOURCE_GROUP` | `rg-interview-app` |
| `AZURE_APP_NAME` | `interview-backend` |
| *(opcional)* `SONAR_TOKEN` / `SNYK_TOKEN` | Si se agregara otro análisis |

**Los secretos del Container App (los que usa la aplicación al arrancar):**

| Secreto | Por qué es secreto |
| :--- | :--- |
| `SPRING_DATASOURCE_PASSWORD` | Da acceso a la base |
| `JWT_SECRET` | Con esta clave se pueden **falsificar tokens válidos** |
| `GROQ_API_KEY` | Es una clave de API, tiene costo por uso |

**Y lo que NO es secreto, aunque lo parezca:**

| Variable | Por qué es común | Valor |
| :--- | :--- | :--- |
| `JWT_EXPIRATION_MS` | Es pública: no da acceso a nada | `604800000` |
| `SPRING_DATASOURCE_URL` | Sin la contraseña no sirve para entrar; saber el host no es una ventaja | `jdbc:mysql://...` |
| `SPRING_DATASOURCE_USERNAME` | Igual: sin la contraseña no alcanza | `...` |
| `PORT`, `HIKARI_MAX_POOL_SIZE` | Configuración, no credenciales | `8080`, `10` |

**La regla para decidir, que es lo que vale la pena que te lleves:** *¿este valor, por sí solo,
le da acceso a alguien o cuesta plata si se filtra?* Si la respuesta es sí → secreto. Si no →
variable común.

**Ojo con el matiz de la URL:** la URL *por sí sola* no es un secreto, pero **si le pusieras el
usuario y la contraseña adentro** (`jdbc:mysql://user:pass@host/...`) pasaría a serlo. Nosotros
la mandamos **sin** credenciales, por eso puede ser una variable común.

### P24. ¿Y cómo se le pasan los datos al Service Principal? ¿En JSON?

**Sí, en un JSON.** Es la salida del comando que crea el Service Principal:

```bash
az ad sp create-for-rbac --name "sp-interview-backend" ... 
```

Devuelve cuatro campos, y esos cuatro van enteros en el secreto `AZURE_CREDENTIALS`:

| Campo | Qué es |
| :--- | :--- |
| `appId` | El identificador de la **aplicación** (el "usuario" del Service Principal) |
| `password` | El **secreto del cliente** (la "contraseña") |
| `tenant` | El directorio donde vive |
| `subscriptionId` | Sobre qué suscripción va a operar |

La acción `azure/login@v3` lee ese JSON y se autentica sin preguntar nada más.

**Qué es un Service Principal, en una frase:** una **identidad para una aplicación**, no para una
persona. Tiene usuario, contraseña y permisos asignables, igual que un usuario — pero no puede
hacer login interactivo, no tiene MFA y **no depende de que nadie esté empleado**: si mañana se
va la persona dueña de la cuenta, el pipeline sigue funcionando.

**Por qué no usar la cuenta personal:** porque habría que guardar tu usuario y contraseña como
secreto de GitHub, y ese secreto tendría los **permisos de toda la suscripción**.

**La evolución natural (mencionarla suma):** OIDC. En vez de una contraseña de larga duración,
GitHub demuestra su identidad a Azure con un token de corta duración, y no hay ningún secreto que
pueda filtrarse. Requiere configuración adicional (federated credentials) y por eso tiene más
sentido en un ambiente productivo con más de un operador. Está documentado en la documentación
del proyecto como la mejora pendiente — queda muy bien decir que se sabe y por qué no está.

### P25. Grype y Snyk: ¿cuál para qué?

Ver [P10](#p10-tengo-que-llevarlo-sin-vulnerabilidades-o-mitigadas) para la tabla completa.
**La forma corta:** Grype para la imagen terminada (dependencias + sistema operativo), CodeQL
para nuestro código. Snyk haría las dos, pero su capa gratuita es limitada y la de CodeQL es
gratis e ilimitada en repositorios públicos.

**Una precisión sobre "Snyk para el pom, Dockerfile y el build":** Snyk lee el `pom.xml`/
`build.gradle` para listar dependencias, y tiene `snyk container test` para la imagen. Analizar
el **Dockerfile** en sí es otra categoría (*IaC / misconfiguration*), que también existe en Snyk
y en Trivy, pero no es análisis de dependencias.

---

# Parte 5 — El guion de la demo

Todo verificado. Si el profesor dice "hacé un cambio y mostrame el camino", seguí esto.

## Paso 0 — Antes de la clase (2 minutos)

```bash
# Encender la base: sin esto, la app no arranca y el health devuelve 503
bash scripts/admin.sh encender

# Confirmar que está todo arriba
bash scripts/admin.sh estado

# Confirmar el estado del repo (tiene que estar limpio)
git status
```

## Paso 1 — Hacer un cambio visible

Un cambio chico pero comprobable. Por ejemplo, en `InterviewController`, o algo que se vea en un
endpoint. **Que el cambio se pueda verificar con un `curl`** — así la demo cierra redonda.

```bash
git add -A
git commit -m "demo: cambio para mostrar el pipeline en vivo"
git push
```

## Paso 2 — Mostrar el pipeline corriendo

GitHub → **Actions** → la ejecución en curso. Ir señalando:

| Paso | Qué decir |
| :--- | :--- |
| 1. Compilar y ejecutar los tests | "Los tests **no** están desactivados. Si fallan, el pipeline se corta acá y no llega a publicar" |
| 2. Construir la imagen | "Las etiquetas las pone el pipeline: el SHA corto siempre, la versión semántica si es un tag" |
| 3. Escanear con Grype | "Escanea la imagen terminada. El informe queda como artefacto descargable" |
| 4. Login en Azure | "Acá entra el Service Principal" |
| 5. Publicar en el ACR | "Esto es Integración Continua: ya está integrado y publicado" |
| — | **"Y acá se detiene: un push a `main` no despliega. Publicar no es liberar"** |

## Paso 3 — Liberar la versión

```bash
git tag v1.1.1
git push origin v1.1.1
```

GitHub → Actions → la nueva ejecución. Ahora sí corre también el job `deploy`, y termina con el
**health check en verde**.

## Paso 4 — Verificar que de verdad funciona

```bash
bash scripts/admin.sh estado     # estado + componentes + URL
bash scripts/admin.sh versiones  # la imagen nueva en el ACR
```

Y una petición real:

```bash
FQDN=$(az containerapp show -n interview-backend -g rg-interview-app \
  --query properties.configuration.ingress.fqdn -o tsv)

curl -i "https://${FQDN}/actuator/health"     # → {"status":"UP"}
```

## Paso 5 — Demostrar el control

| Qué mostrar | Cómo |
| :--- | :--- |
| **Reversión** | `bash scripts/admin.sh` → opción 3 → elegir la etiqueta anterior |
| **Estado de la app** | `bash scripts/admin.sh app` |
| **Detener** | `bash scripts/admin.sh app-frenar` (o el portal) |
| **Encender** | `bash scripts/admin.sh app-iniciar` |
| **Ver el historial** | `bash scripts/admin.sh revisiones` |

## Las tres frases que cierran la demo

1. **"Publicar no es liberar."** Un push a `main` construye y publica; solo un tag libera. Eso
   permite tener una versión lista sin que esté corriendo.
2. **"El pipeline no declara éxito hasta que la aplicación responde."** No alcanza con que la
   imagen se suba: el último paso consulta el health check y espera un 200.
3. **"El código no sabe dónde corre."** Ninguna credencial está en el repositorio; la misma
   imagen corre igual en tu máquina y en Azure, solo cambian las variables de entorno.

---

## Fuentes

- [Microsoft Learn — Update and deploy changes in Azure Container Apps (revisiones)](https://learn.microsoft.com/en-us/azure/container-apps/revisions)
- [Microsoft Learn — Manage the Container App lifecycle](https://learn.microsoft.com/en-us/training/modules/manage-containers-azure-container-apps/3-app-lifecycle-management)
- [Microsoft Learn — Scaling in Azure Container Apps](https://learn.microsoft.com/en-us/azure/container-apps/scale-app)
