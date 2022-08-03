

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.4.1"
  kotlin("plugin.spring") version "1.7.10"
  kotlin("plugin.jpa") version "1.7.10"
  id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
  jacoco
  java
  id("io.gitlab.arturbosch.detekt").version("1.20.0")
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
  }
}
val cucumberVersion by extra("7.3.3")
val springDocVersion by extra("1.6.9")

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  runtimeOnly("org.postgresql:postgresql:42.4.0")
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
  implementation("com.vladmihalcea:hibernate-types-52:2.17.1")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("com.google.code.gson:gson")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.1.7")

  implementation("com.opencsv:opencsv:5.6")

  testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(module = "mockito-core")
  }
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.mock-server:mockserver-netty:5.13.2")

  testImplementation("com.ninja-squad:springmockk:3.1.1")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
  testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
  testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
  testImplementation("org.junit.platform:junit-platform-console:1.9.0")
}

jacoco {
  toolVersion = "0.8.8"
}

detekt {
  config = files("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
  ignoreFailures = true
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
          minimum = BigDecimal(0.89)
        }
        limit {
          counter = "COMPLEXITY"
          minimum = BigDecimal(0.90)
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
