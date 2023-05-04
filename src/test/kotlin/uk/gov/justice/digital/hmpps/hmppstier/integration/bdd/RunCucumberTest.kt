package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("uk/gov/justice/digital/hmpps/hmppstier/integration/bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, junit:build/test-results/TEST-cucumber.xml, uk.gov.justice.digital.hmpps.hmppstier.integration.bdd.MockServerPlugin")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "uk.gov.justice.digital.hmpps.hmppstier.integration.bdd")
class RunCucumberTest