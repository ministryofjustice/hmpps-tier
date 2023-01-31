

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.8.1"
  kotlin("plugin.spring") version "1.8.0"
  kotlin("plugin.jpa") version "1.8.0"
  id("org.jlleitschuh.gradle.ktlint") version "11.1.0"
  jacoco
  java
  id("io.gitlab.arturbosch.detekt").version("1.22.0")
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
  }
}
val cucumberVersion by extra("7.11.0")
val springDocVersion by extra("1.6.14")

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  runtimeOnly("org.postgresql:postgresql:42.5.1")
  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")

  implementation("org.springframework:spring-webflux")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")

  implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-kotlin:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-data-rest:$springDocVersion")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.vladmihalcea:hibernate-types-52:2.21.1")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.2.0")

  testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(module = "mockito-core")
  }
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")

  testImplementation("com.ninja-squad:springmockk:4.0.0")
  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
  testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
  testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
  testImplementation("org.junit.platform:junit-platform-console:1.9.2")
}

jacoco {
  toolVersion = "0.8.8"
}

detekt {
  config = files("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
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
    dependsOn(":ktlintCheck", detekt)
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
          counter = "BRANCH"
          minimum = BigDecimal(0.87)
        }
        limit {
          counter = "COMPLEXITY"
          minimum = BigDecimal(0.91)
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
      jvmTarget = JavaVersion.VERSION_18.toString()
    }
  }
  compileTestKotlin {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_18.toString()
    }
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}
