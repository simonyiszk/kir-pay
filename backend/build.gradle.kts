plugins {
  kotlin("jvm") version "2.4.10"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
  id("org.springframework.boot") version "4.1.1"
  id("io.spring.dependency-management") version "1.1.7"
  id("jacoco")
}

group = "hu.bme.sch"
version = "1.1.0"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("tools.jackson.module:jackson-module-kotlin")
  implementation("tools.jackson.dataformat:jackson-dataformat-csv")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  runtimeOnly("io.micrometer:micrometer-registry-prometheus")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  implementation("org.postgresql:postgresql")
  implementation("org.springframework.boot:spring-boot-flyway")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  implementation("org.springframework.boot:spring-boot-starter-session-jdbc")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-test")
  testImplementation("io.mockk:mockk:1.14.11")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required = true
    html.required = true
  }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
  builder = "paketobuildpacks/builder-jammy-tiny:latest"

  environment = mapOf(
    "BP_NATIVE_IMAGE" to "false",
    "BP_JVM_AOTCACHE_ENABLED" to "true",
    "BP_SPRING_AOT_ENABLED" to "false",
    "BP_JVM_VERSION" to java.toolchain.languageVersion.get().asInt().toString(),

    "LC_ALL" to "en_US.UTF-8",
    "BPE_LC_ALL" to "en_US.UTF-8",

    "BPE_BPL_JVM_THREAD_COUNT" to "50",
    "BPE_BPL_JVM_HEAD_ROOM" to "5",
    "BPE_BPL_JVM_LOADED_CLASS_COUNT" to "38000",

    "TRAINING_RUN_JAVA_TOOL_OPTIONS" to "-XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders -Dspring.profiles.active=cds-training",

    "BPE_PREPEND_JAVA_TOOL_OPTIONS" to "-XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders",
    "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
    "BPE_APPEND_JAVA_TOOL_OPTIONS" to "-XX:ReservedCodeCacheSize=30M -Xss200K -Xlog:cds=info -Xlog:aot=info -Xlog:class+path=info -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8",
  )
}
