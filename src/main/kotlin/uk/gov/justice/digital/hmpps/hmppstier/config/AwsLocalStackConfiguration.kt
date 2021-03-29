package uk.gov.justice.digital.hmpps.hmppstier.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@EnableSqs
@Configuration
@ConditionalOnProperty(name = ["offender-events.sqs-provider"], havingValue = "localstack")
class AwsLocalStackConfiguration(
  @Value("\${aws.offender.region}") val region: String
) {

  @Primary
  @Bean(name = ["offenderEvents"])
  fun offenderEventsAmazonSQSAsync(
    @Value("\${offender-events.sqs-endpoint}") serviceEndpoint: String
  ): AmazonSQSAsync {
    return AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()
  }

  @Bean
  fun calculationCompleteEventsAmazonSQSAsync(
    @Value("\${calculation-complete.sqs-endpoint}") serviceEndpoint: String
  ): AmazonSQSAsync {
    return AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()
  }

  @Primary
  @Bean(name = ["hmppsDomainEvents"])
  fun hmppsDomainEventsAmazonSNSAsync(
    @Value("\${hmpps-domain-events.sns-endpoint}") serviceEndpoint: String
  ): AmazonSNSAsync {
    return AmazonSNSAsyncClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("key", "secret")))
      .build()
  }

  @Primary
  @Bean
  fun simpleMessageListenerContainerFactory(amazonSQSAsync: AmazonSQSAsync):
    SimpleMessageListenerContainerFactory {
      val factory = SimpleMessageListenerContainerFactory()
      factory.setAmazonSqs(amazonSQSAsync)
      factory.setMaxNumberOfMessages(1)
      factory.setWaitTimeOut(20)
      return factory
    }
}
