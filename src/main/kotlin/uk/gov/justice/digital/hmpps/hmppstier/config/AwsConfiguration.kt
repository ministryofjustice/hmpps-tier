package uk.gov.justice.digital.hmpps.hmppstier.config

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
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
@ConditionalOnProperty(name = ["offender-events.sqs-provider"], havingValue = "aws")
class AwsConfiguration(
  @Value("\${aws.offender.access-key-id}") val offenderEventsAccessKeyId: String,
  @Value("\${aws.offender.secret-access-key}") val offenderEventsSecretKey: String,
  @Value("\${aws.offender.region}") val offenderEventsRegion: String,
  @Value("\${aws.hmpps-domain.access-key-id}") val hmppsDomainEventsAccessKeyId: String,
  @Value("\${aws.hmpps-domain.secret-access-key}") val hmppsDomainEventsSecretKey: String,
  @Value("\${aws.hmpps-domain.region}") val hmppsDomainEventsRegion: String
) {

  @Primary
  @Bean(name = ["offenderEvents"])
  fun offenderEventsAmazonSQSAsync(): AmazonSQSAsync {
    val credentials: AWSCredentials = BasicAWSCredentials(offenderEventsAccessKeyId, offenderEventsSecretKey)
    return AmazonSQSAsyncClientBuilder
      .standard()
      .withRegion(offenderEventsRegion)
      .withCredentials(AWSStaticCredentialsProvider(credentials)).build()
  }

  @Primary
  @Bean(name = ["hmmpsDomainEvents"])
  fun hmppsDomainEventsAmazonSNSAsync(): AmazonSNSAsync {
    val credentials: AWSCredentials = BasicAWSCredentials(hmppsDomainEventsAccessKeyId, hmppsDomainEventsSecretKey)
    return AmazonSNSAsyncClientBuilder
      .standard()
      .withRegion(hmppsDomainEventsRegion)
      .withCredentials(AWSStaticCredentialsProvider(credentials)).build()
  }

  @Primary
  @Bean
  fun simpleMessageListenerContainerFactory(amazonSQSAsync: AmazonSQSAsync):
    SimpleMessageListenerContainerFactory {
      val factory = SimpleMessageListenerContainerFactory()
      factory.setAmazonSqs(amazonSQSAsync)
      factory.setMaxNumberOfMessages(2)
      factory.setWaitTimeOut(20)
      return factory
    }
}
