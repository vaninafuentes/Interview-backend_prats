plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

// Sobreescrituras de versiones gestionadas por el BOM, para cerrar
// vulnerabilidades detectadas con Grype (detalle en docs/seguridad.md):
//   - Tomcat 10.1.60: el BOM fija 10.1.55, que deja 3 CVE críticas sin resolver
//   - Jackson 2.21.7: el BOM fija 2.21.4, con 11 advisories corregidos desde 2.21.5
extra["tomcat.version"] = "10.1.60"
extra["jackson-bom.version"] = "2.21.7"

springBoot {
    mainClass.set("com.example.interviewback.InterviewBackApplication")
    buildInfo()   // publica la versión y la fecha de build en /actuator/info
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        // El host de desarrollo tiene Java 26; el contenedor y el CI, Java 21 LTS.
        val currentJavaMajor = System.getProperty("java.version").split(".")[0].toIntOrNull() ?: 21
        languageVersion = JavaLanguageVersion.of(if (currentJavaMajor >= 26) 26 else 21)
    }
}

// Bytecode de Java 21, para que coincida con el runtime de la imagen Docker.
tasks.withType<JavaCompile> {
    options.release.set(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    runtimeOnly("com.mysql:mysql-connector-j")

    // Neutraliza caracteres de control en los logs (CWE-117, alerta de CodeQL).
    implementation("org.owasp.encoder:encoder:1.5.0")

    // Tokens JWT firmados con HMAC-SHA
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
