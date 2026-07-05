plugins {
  kotlin("jvm") version "2.4.0"
  kotlin("plugin.spring") version "2.4.0"
  id("org.springframework.boot") version "4.1.0"
  id("io.spring.dependency-management") version "1.1.7"
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
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework:spring-aspects")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.springframework.modulith:spring-modulith-starter-core")
  implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  runtimeOnly("org.postgresql:postgresql")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.0")
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
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
