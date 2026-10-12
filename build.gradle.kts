/*
 * build.gradle.kts
 * ----------------
 * Archivo principal de build del proyecto.
 *
 * Plugins:
 *   - java               → compila código Java
 *   - spring-boot         → empaqueta como JAR ejecutable y configura bootRun
 *   - dependency-management → alinea versiones de dependencias de Spring
 *
 * Dependencias clave:
 *   - spring-boot-starter-web      → REST controllers, embedded Tomcat
 *   - spring-boot-starter-data-jpa → JPA/Hibernate para persistencia
 *   - mysql-connector-j            → driver JDBC de MySQL (solo en runtime)
 */

plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

/*
 * Remediación de vulnerabilidades (análisis Grype) — ver docs/seguridad.md
 * ---------------------------------------------------------------------------
 * El BOM de Spring Boot 3.5.16 fija Tomcat 10.1.55, que corrige 3 CVE críticas
 * pero deja sin resolver otras 3 (GHSA-9xv2-5v5q-p794, GHSA-gcx9-497g-6cp6,
 * GHSA-h3x4-894j-xpx5) que requieren 10.1.58+.
 * Spring Boot permite sobreescribir la versión gestionada (documentado como
 * "override managed dependency version"); 10.1.60 es la última 10.1.x y
 * permanece dentro de la misma línea, por lo que no rompe compatibilidad.
 */
extra["tomcat.version"] = "10.1.60"

/*
 * El BOM de 3.5.16 fija Jackson 2.21.4, que arrastra 11 advisories (jackson-core
 * y jackson-databind, severidad High/Medium) con fix publicado en 2.21.5-2.21.7.
 * Se sobreescribe a 2.21.7: misma línea 2.21.x (patch), sin cambios de API.
 */
extra["jackson-bom.version"] = "2.21.7"

springBoot {
    mainClass.set("com.example.interviewback.InterviewBackApplication")
    // Genera META-INF/build-info.properties, que Actuator publica en /actuator/info.
    // Permite saber qué versión está corriendo consultando la aplicación
    // (útil para comparar con la etiqueta de imagen desplegada en Azure).
    buildInfo()
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        // Detecta dinámicamente si corre en host con Java 26 o en contenedor/CI con Java 21 LTS
        val currentJavaMajor = System.getProperty("java.version").split(".")[0].toIntOrNull() ?: 21
        languageVersion = JavaLanguageVersion.of(if (currentJavaMajor >= 26) 26 else 21)
    }
}

tasks.withType<JavaCompile> {
    // Generamos bytecode de Java 21 para compatibilidad universal con el runtime y Spring Boot
    options.release.set(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Spring Boot starters ---
    // Web: habilita REST controllers, serialización JSON (Jackson), Tomcat embebido
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Data JPA: integra Hibernate como ORM + Spring Data repositories
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Actuator: observabilidad, métricas y health checks para Docker y Azure
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- Base de datos ---
    // Driver MySQL: solo se necesita en runtime, no en compilación
    runtimeOnly("com.mysql:mysql-connector-j")

    // --- Seguridad ---
    // Spring Security: BCrypt, filtros HTTP, autenticación
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Validación de DTOs: @NotBlank, @Email, @Size, etc.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OWASP Java Encoder: neutraliza caracteres de control antes de escribir en
    // logs (previene la "inyección de registros", CWE-117, detectada por CodeQL).
    // Es la solución estándar recomendada por OWASP para este problema, pesa
    // 38 KB y no tiene dependencias propias.
    implementation("org.owasp.encoder:encoder:1.5.0")

    // JWT: generación y validación de tokens firmados con HS256
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- Testing ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
