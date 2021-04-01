package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.amazonaws.services.sns.AmazonSNSAsync
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["offender-events.sqs-provider"], havingValue = "mocked")
class AwsLocalStackSQSConfiguration() {

  @MockBean
  var simpleMessageListenerContainer: SimpleMessageListenerContainer? = null

  @MockBean
  var hmppsDomainEventsAmazonSNSAsync: AmazonSNSAsync? = null
}
