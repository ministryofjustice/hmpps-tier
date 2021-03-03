
plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.1.1"
  kotlin("plugin.spring") version "1.4.30"
  kotlin("plugin.jpa") version "1.4.30"
  id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("com.zaxxer:HikariCP:3.4.5")
  runtimeOnly("org.flywaydb:flyway-core:6.5.6")

  implementation("org.springframework:spring-webflux")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")
  implementation("com.vladmihalcea:hibernate-types-52:2.10.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.springframework.cloud:spring-cloud-aws-messaging")

  testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(module = "mockito-core")
  }
  testImplementation("org.mock-server:mockserver-netty:5.11.1")

  testImplementation("com.ninja-squad:springmockk:2.0.1")
  testImplementation("org.assertj:assertj-core:3.18.0")
}

extra["springCloudVersion"] = "Hoxton.SR8"

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
  }
}

jacoco {
  toolVersion = "0.8.6"
}

tasks.jacocoTestReport {
  reports {
    xml.isEnabled = false
    csv.isEnabled = false
    html.destination = file("$buildDir/reports/coverage")
  }
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = BigDecimal(0.81)  
      }
      limit {
        counter = "BRANCH"
        minimum = BigDecimal(0.79)
      }
      limit {
        counter = "COMPLEXITY"
        minimum = BigDecimal(0.75)
      }
    }
  }
}

tasks.named("check") {
  dependsOn(":ktlintCheck")
  finalizedBy("jacocoTestReport")
}

tasks.named("jacocoTestReport") {
  dependsOn("test")
}

tasks.named("jacocoTestCoverageVerification") {
  dependsOn("jacocoTestReport")
}

tasks.register("fix") {
  dependsOn(":ktlintFormat")
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,localstack")
}
