val cucumberVersion = "7.1.0"

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.1"
  kotlin("plugin.spring") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
  jacoco
  java
  id("io.gitlab.arturbosch.detekt").version("1.19.0")
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  runtimeOnly("org.postgresql:postgresql:42.3.2")
  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")

  implementation("org.springframework:spring-webflux")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")

  implementation("org.springdoc:springdoc-openapi-ui:1.5.11")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.11")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.6")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.vladmihalcea:hibernate-types-52:2.10.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("com.google.code.gson:gson")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.3")

  testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(module = "mockito-core")
  }
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.mock-server:mockserver-netty:5.11.1")

  testImplementation("com.ninja-squad:springmockk:2.0.1")
  testImplementation("org.assertj:assertj-core:3.18.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.0.3")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
  testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
  testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
  testImplementation("org.junit.platform:junit-platform-console:1.7.1")
}

jacoco {
  toolVersion = "0.8.7"
}

detekt {
  config = files("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
  ignoreFailures = true
}

tasks {
  val cucumber by registering(JavaExec::class) {
    dependsOn(testClasses)
    finalizedBy("jacocoTestCoverageVerification")
    val reportsDir = file("$buildDir/test-results")
    outputs.dir(reportsDir)
    classpath = sourceSets["test"].runtimeClasspath
    main = "org.junit.platform.console.ConsoleLauncher"
    args("--include-classname", ".*")
    args("--select-class", "uk.gov.justice.digital.hmpps.hmppstier.integration.bdd.CucumberRunnerTest")
    args("--exclude-tag", "disabled")

    // if you want to run one feature/scenario, tag it @single and uncomment
    // args("--include-tag", "single")
    args("--reports-dir", reportsDir)
    systemProperty("cucumber.publish.quiet", true)
    val jacocoAgent = zipTree(configurations.jacocoAgent.get().singleFile)
      .filter { it.name == "jacocoagent.jar" }
      .singleFile
    jvmArgs = listOf("-javaagent:$jacocoAgent=destfile=$buildDir/jacoco/cucumber.exec,append=false")
  }

  getByName("check") {
    dependsOn(":ktlintCheck", detekt)
    finalizedBy(cucumber)
  }
  getByName<JacocoReport>("jacocoTestReport") {
    executionData(files("$buildDir/jacoco/cucumber.exec", "$buildDir/jacoco/test.exec"))
    reports {
      xml.isEnabled = false
      csv.isEnabled = false
      html.destination = file("$buildDir/reports/coverage")
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
      jvmTarget = JavaVersion.VERSION_16.toString()
    }
  }
  compileTestKotlin {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_16.toString()
    }
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}
