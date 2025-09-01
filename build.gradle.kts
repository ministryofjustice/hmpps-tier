import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.7"
    kotlin("plugin.spring") version "2.2.10"
    kotlin("plugin.jpa") version "2.2.10"
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

    runtimeOnly("org.postgresql:postgresql:42.7.7")
    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.11")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")

    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.20.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mock-server:mockserver-netty:5.15.0")

    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
    testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
    testImplementation("io.cucumber:cucumber-spring:7.27.2")
    testImplementation("io.cucumber:cucumber-java8:7.27.2")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.27.2")
    testImplementation("org.junit.platform:junit-platform-console:1.13.4") // Manually set to 1.13.4 for cucumber 7.26.0
    testImplementation("org.junit.platform:junit-platform-launcher:1.13.4") // Manually set to 1.13.4 for cucumber 7.26.0
}

// Manually set to 5.13.3 for cucumber 7.26.0. Should be able to be removed when Spring updates to JUnit 5.13.3
ext["junit-jupiter.version"] = "5.13.3"

jacoco {
    toolVersion = "0.8.12"
}

task("cucumber") {
    dependsOn("assemble", "testClasses")
    finalizedBy("jacocoTestCoverageVerification")
    doLast {
        javaexec {
            mainClass.set("io.cucumber.core.cli.Main")
            classpath = sourceSets["test"].runtimeClasspath
            val jacocoAgent = zipTree(configurations.jacocoAgent.get().singleFile)
                .filter { it.name == "jacocoagent.jar" }
                .singleFile
            jvmArgs =
                listOf("-javaagent:$jacocoAgent=destfile=${layout.buildDirectory.get().asFile}/jacoco/cucumber.exec,append=false")
        }
    }
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
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
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