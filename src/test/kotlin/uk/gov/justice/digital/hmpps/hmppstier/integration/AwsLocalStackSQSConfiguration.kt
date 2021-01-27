package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.localstack.LocalStackContainer

@Configuration
@ConditionalOnProperty(name = ["offender-events.sqs-provider"], havingValue = "localstack-embedded")
class AwsLocalStackSQSConfiguration(private val localStackContainer: LocalStackContainer) {

  @Primary
  @Bean
  fun localstackTestClient(localStackContainer: LocalStackContainer): AmazonSQSAsync {
    return AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
      .withCredentials(localStackContainer.defaultCredentialsProvider)
      .build()
  }
}
