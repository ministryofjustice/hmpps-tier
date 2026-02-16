import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.3"
    kotlin("plugin.spring") version "2.3.10"
    kotlin("plugin.jpa") version "2.3.10"
    jacoco
}

configurations {
    implementation { exclude(module = "applicationinsights-spring-boot-starter") }
    implementation { exclude(module = "applicationinsights-logging-logback") }
    testImplementation {
        exclude(group = "org.junit.vintage")
    }
}

dependencyCheck {
    suppressionFiles.add("suppressions.xml")
}

repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {

    runtimeOnly("org.postgresql:postgresql:42.7.10")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.15.2")

    implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.0.0")

    implementation(platform("io.sentry:sentry-bom:8.32.0"))
    implementation("io.sentry:sentry-spring-boot-4")
    implementation("io.sentry:sentry-logback")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.mock-server:mockserver-netty:5.15.0")

    testImplementation("com.ninja-squad:springmockk:5.0.1")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
    testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
    testImplementation("io.cucumber:cucumber-spring:7.34.2")
    testImplementation("io.cucumber:cucumber-java8:7.34.2")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.34.2")
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.register<JavaExec>("cucumber") {
    dependsOn("assemble", "testClasses")
    finalizedBy("jacocoTestCoverageVerification")
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = sourceSets["test"].runtimeClasspath
    val jacocoAgent = zipTree(configurations.jacocoAgent.get().singleFile)
        .filter { it.name == "jacocoagent.jar" }
        .singleFile
    jvmArgs =
        listOf("-javaagent:$jacocoAgent=destfile=${layout.buildDirectory.get().asFile}/jacoco/cucumber.exec,append=false")
}

tasks {

    getByName("check") {
        finalizedBy("cucumber")
    }
    getByName<JacocoReport>("jacocoTestReport") {
        executionData(
            files(
                "${layout.buildDirectory.get().asFile}/jacoco/cucumber.exec",
                "${layout.buildDirectory.get().asFile}/jacoco/test.exec"
            )
        )
        reports {
            xml.required.set(false)
            csv.required.set(false)
            html.outputLocation.set(file("${layout.buildDirectory.get().asFile}/reports/coverage"))
        }
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it) {
                            exclude("**/config/**")
                        }
                    }
                )
            )
        }
    }
    getByName<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        executionData(
            "${layout.buildDirectory.get().asFile}/jacoco/cucumber.exec",
            "${layout.buildDirectory.get().asFile}/jacoco/test.exec"
        )
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    minimum = BigDecimal(0.80)
                }
                limit {
                    counter = "COMPLEXITY"
                    minimum = BigDecimal(0.80)
                }
            }
        }
        dependsOn("jacocoTestReport")
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it) {
                            exclude(
                                "**/config/**",
                                "**/client/NeedSection**",
                                "**/service/RecalculationSource**",
                                "**/cronjob/**"
                            )
                        }
                    }
                )
            )
        }
    }

    getByName<Test>("test") {
        exclude("**/CucumberRunnerTest*")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

tasks.named<JavaExec>("bootRun") {
    systemProperty("spring.profiles.active", "dev,docker")
}

// Disable ktlint in favour of IntelliJ formatting
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/*")
    }
}