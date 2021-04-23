package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import io.cucumber.junit.CucumberOptions
import io.cucumber.junit.platform.engine.Cucumber

@CucumberOptions(
  strict = true,
  glue = ["features"],
  stepNotifications = true,
  features = ["src/test/resources/features"],
  plugin = ["pretty"]
)
@Cucumber
class CucumberRunnerTest
