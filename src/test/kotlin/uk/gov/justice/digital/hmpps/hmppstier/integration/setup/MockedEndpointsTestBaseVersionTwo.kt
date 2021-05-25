package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test-version-two")
abstract class MockedEndpointsTestBaseVersionTwo : MockedEndpointsTestBase()
