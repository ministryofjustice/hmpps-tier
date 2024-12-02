plugins {
    id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"
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

val cucumberVersion by extra("7.18.0")
val springDocVersion by extra("1.6.14")

repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {

    runtimeOnly("org.postgresql:postgresql:42.7.4")
    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.0")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.0")

    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mock-server:mockserver-netty:5.15.0")

    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
    testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
    testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("org.junit.platform:junit-platform-console:1.11.3")
}

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
            jvmArgs = listOf("-javaagent:$jacocoAgent=destfile=$buildDir/jacoco/cucumber.exec,append=false")
        }
    }
}

tasks {

    getByName("check") {
        finalizedBy("cucumber")
    }
    getByName<JacocoReport>("jacocoTestReport") {
        executionData(files("$buildDir/jacoco/cucumber.exec", "$buildDir/jacoco/test.exec"))
        reports {
            xml.required.set(false)
            csv.required.set(false)
            html.outputLocation.set(file("$buildDir/reports/coverage"))
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
        executionData("$buildDir/jacoco/cucumber.exec", "$buildDir/jacoco/test.exec")
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
                            exclude("**/config/**")
                        }
                    }
                )
            )
        }
    }

    getByName<Test>("test") {
        exclude("**/CucumberRunnerTest*")
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_21.toString()
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_21.toString()
        }
    }
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