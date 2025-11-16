import java.time.Duration

plugins {
    // Kotlin plugins
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"  // All-open for Spring
    kotlin("plugin.jpa") version "2.0.21"      // No-arg for JPA

    // Spring Boot
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"

    // Testing & Quality
    id("io.qameta.allure") version "3.0.1"
    jacoco
}

group = "solid.humank"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        // Enable strict null safety
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",           // Strict Java interop null-safety
            "-Xemit-jvm-type-annotations", // Emit type annotations
            "-java-parameters"           // Preserve parameter names
        )

        // Enable Kotlin features
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
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

// Allure 配置
allure {
    version.set("2.22.1")
    useJUnit5 {
        version.set("2.22.1")
    }
}

// 定義 Allure 結果目錄
val allureResultsDir = layout.buildDirectory.dir("allure-results").get().asFile.absolutePath

// JaCoCo 配置
jacoco {
    toolVersion = "0.8.12"
}

// JaCoCo 測試報告配置
tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/entity/**",
                    "**/exception/**",
                    "**/Application.class",
                    "**/*Kt.class"  // Kotlin generated classes
                )
            }
        })
    )
}

// JaCoCo 覆蓋率驗證配置
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            enabled = true
            element = "CLASS"

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }

            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }

        rule {
            enabled = true
            element = "PACKAGE"

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/entity/**",
                    "**/exception/**",
                    "**/Application.class",
                    "**/*Kt.class"
                )
            }
        })
    )
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kotlin Standard Library & Coroutines (for future use)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    // Jackson Kotlin Module (for JSON serialization)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Redis and Redisson for distributed locking
    implementation("org.redisson:redisson-spring-boot-starter:3.52.0")
    implementation("org.apache.commons:commons-pool2:2.12.1")

    // Resilience4j for circuit breaker pattern
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.3.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.3.0")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.3.0")  // Kotlin extensions

    // Micrometer for metrics collection
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-registry-cloudwatch2")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    // OpenTelemetry for distributed tracing
    implementation("io.opentelemetry:opentelemetry-api:1.56.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.56.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.56.0")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.37.0")

    // AWS X-Ray support for production
    implementation("com.amazonaws:aws-xray-recorder-sdk-core:2.20.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
    }
    implementation("com.amazonaws:aws-xray-recorder-sdk-spring:2.20.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
    }

    // AWS SDK v2 for analytics and security
    implementation(platform("software.amazon.awssdk:bom:2.38.2"))
    implementation("software.amazon.awssdk:firehose")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:cloudwatch")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")

    // Unified HttpComponents5 dependencies
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5.1")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.3.6")
    implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.3.6")

    // Logback encoder for structured logging
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    // OpenAPI 3 (Swagger) 支援
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

    // Testing Dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Kotest for Kotlin testing
    testImplementation("io.kotest:kotest-runner-junit5:6.0.0.M1")
    testImplementation("io.kotest:kotest-assertions-core:6.0.0.M1")
    testImplementation("io.kotest:kotest-property:6.0.0.M1")
    testImplementation("io.kotest:kotest-extensions-spring:6.0.0.M1")
    testImplementation("io.kotest:kotest-framework-datatest:6.0.0.M1")

    // MockK for Kotlin mocking
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // HTTP Client for tests
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.5.1")
    testImplementation("org.apache.httpcomponents.core5:httpcore5:5.3.6")
    testImplementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.5.1")

    // Cucumber for BDD testing
    testImplementation("io.cucumber:cucumber-java:7.31.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.31.0")
    testImplementation("io.cucumber:cucumber-spring:7.31.0")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.junit.platform:junit-platform-commons:6.0.1")
    testImplementation("org.junit.platform:junit-platform-engine:6.0.1")
    testImplementation("org.junit.platform:junit-platform-launcher:6.0.1")

    // Mockito (for Java interop during migration)
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.5.0")  // Kotlin DSL

    // Allure 依賴
    testImplementation("io.qameta.allure:allure-junit5:2.30.0")
    testImplementation("io.qameta.allure:allure-cucumber7-jvm:2.30.0")
    testImplementation("io.qameta.allure:allure-java-commons:2.30.0")
    testImplementation("io.qameta.allure:allure-kotlin-model:2.30.0")
    testImplementation("io.qameta.allure:allure-kotlin-commons:2.30.0")

    // ArchUnit for architecture testing
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    // Arrow for Functional Programming (optional, for advanced usage)
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
}

// Kotlin 編譯任務配置
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(
            "--enable-preview",
            "-Xjsr305=strict",
            "-Xemit-jvm-type-annotations"
        )
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// 測試任務配置
tasks.withType<Test> {
    jvmArgs("--enable-preview")
    useJUnitPlatform()

    // 基本記憶體配置
    maxHeapSize = "4g"
    minHeapSize = "1g"

    // 基本JVM參數
    jvmArgs(
        "-XX:MaxMetaspaceSize=1g",
        "-XX:+UseG1GC",
        "-Xshare:off"
    )

    // 測試執行配置
    maxParallelForks = 1
    forkEvery = 5

    // 測試執行超時配置
    timeout.set(Duration.ofMinutes(15))

    // 基本系統屬性
    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.root", "ERROR")
    systemProperty("spring.jmx.enabled", "false")
    systemProperty("spring.main.lazy-initialization", "true")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}

// ============================================================================
// 分層測試任務配置 - 遵循測試金字塔原則
// ============================================================================

// 快速測試任務 - 即時反饋 (< 2分鐘)
val quickTest by tasks.registering(Test::class) {
    description = "Fast unit tests for immediate feedback during development (< 2 minutes)"
    group = "verification"

    useJUnitPlatform {
        excludeTags("integration", "end-to-end", "slow", "external")
        includeTags("unit", "fast")
    }

    maxHeapSize = "1g"
    minHeapSize = "256m"
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    forkEvery = 0

    jvmArgs(
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-XX:+UseStringDeduplication",
        "-XX:G1HeapRegionSize=16m",
        "-Djunit.jupiter.execution.parallel.enabled=true",
        "-Djunit.jupiter.execution.parallel.mode.default=concurrent",
        "-Djunit.jupiter.execution.parallel.config.strategy=dynamic"
    )

    timeout.set(Duration.ofMinutes(2))

    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }

    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.root", "ERROR")
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
}

// 完整單元測試任務 - 提交前驗證 (< 5分鐘)
val unitTest by tasks.registering(Test::class) {
    description = "Comprehensive unit tests for pre-commit validation (< 5 minutes)"
    group = "verification"

    useJUnitPlatform {
        excludeTags("integration", "slow")
        includeTags("unit")
    }

    maxHeapSize = "2g"
    minHeapSize = "512m"
    maxParallelForks = 2
    forkEvery = 0

    jvmArgs(
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-XX:+UseStringDeduplication",
        "-XX:G1HeapRegionSize=16m",
        "-Djunit.jupiter.execution.parallel.enabled=true"
    )

    timeout.set(Duration.ofMinutes(5))

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }

    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.root", "ERROR")
    systemProperty("junit.jupiter.execution.timeout.default", "1m")
}

// 集成測試任務 - 提交前使用
val integrationTest by tasks.registering(Test::class) {
    description = "Integration tests for pre-commit verification"
    group = "verification"

    useJUnitPlatform {
        includeTags("integration")
        excludeTags("end-to-end", "slow")
    }

    maxHeapSize = "6g"
    minHeapSize = "2g"
    maxParallelForks = 1
    forkEvery = 5

    timeout.set(Duration.ofMinutes(30))

    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }

    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.root", "ERROR")
}

// 端到端測試任務 - 發布前使用
val e2eTest by tasks.registering(Test::class) {
    description = "End-to-end tests for pre-release verification"
    group = "verification"

    useJUnitPlatform {
        includeTags("end-to-end")
    }

    maxHeapSize = "8g"
    minHeapSize = "3g"
    maxParallelForks = 1
    forkEvery = 2

    timeout.set(Duration.ofHours(1))

    testLogging {
        events("failed", "skipped", "passed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.root", "INFO")
}

// Cucumber 測試任務
val cucumber by tasks.registering(JavaExec::class) {
    dependsOn(tasks.assemble, tasks.testClasses)
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = configurations.testRuntimeClasspath.get() + sourceSets.main.get().output + sourceSets.test.get().output

    maxHeapSize = "4g"
    jvmArgs("--enable-preview", "-Xshare:off")

    args(
        "--plugin", "progress",
        "--plugin", "html:build/reports/cucumber/cucumber-report.html",
        "--plugin", "json:build/reports/cucumber/cucumber-report.json",
        "--glue", "solid.humank.genaidemo.bdd",
        "src/test/resources/features"
    )

    systemProperty("logging.level.root", "ERROR")
}

// ============================================================================
// 便捷的測試任務組合
// ============================================================================

val preCommitTest by tasks.registering {
    dependsOn(unitTest, integrationTest)
    description = "提交前測試 - 包含單元測試和集成測試 (< 5分鐘)"
    group = "verification"

    doLast {
        println("✅ 提交前測試完成 - 單元測試和集成測試通過")
    }
}

val fullTest by tasks.registering {
    dependsOn(unitTest, integrationTest, e2eTest, cucumber)
    description = "完整測試 - 發布前使用，包含所有測試類型"
    group = "verification"

    doLast {
        println("✅ 完整測試完成 - 所有測試通過")
    }
}

// Allure 報告準備
val prepareAllureResults by tasks.registering {
    description = "準備 Allure 報告數據"
    group = "reporting"

    doLast {
        // 確保 allure-results 目錄存在
        file("${layout.buildDirectory.get()}/allure-results").mkdirs()

        // 創建 executor.json 文件
        val executorFile = file("${layout.buildDirectory.get()}/allure-results/executor.json")
        executorFile.writeText("""
            {
                "name": "Gradle",
                "type": "gradle",
                "buildName": "GenAI Demo Project (Kotlin)",
                "reportName": "DDD 架構測試報告"
            }
        """.trimIndent())

        println("✅ Allure 報告數據準備完成")
    }
}

// 清理 Allure 結果目錄
val cleanAllureResults by tasks.registering(Delete::class) {
    delete(allureResultsDir)
    description = "清理 Allure 結果目錄"
    group = "build"
}

// 創建堆轉儲目錄
val createHeapDumpDir by tasks.registering {
    description = "創建堆轉儲目錄"
    group = "build"

    doLast {
        file("${layout.buildDirectory.get()}/reports/heap-dumps").mkdirs()
        println("✅ 堆轉儲目錄已創建")
    }
}

tasks.named("clean") {
    dependsOn(cleanAllureResults)
}

tasks.withType<Test> {
    dependsOn(createHeapDumpDir)
}

// Allure 報告生成
tasks.named("allureReport") {
    dependsOn(prepareAllureResults)
}
