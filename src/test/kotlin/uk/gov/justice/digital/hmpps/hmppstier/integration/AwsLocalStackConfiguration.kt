package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

@Configuration
@ConditionalOnProperty(name = ["offender-events.sqs-provider"], havingValue = "localstack")
class AwsLocalStackConfiguration {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun localStackContainer(): LocalStackContainer {
    log.info("Starting localstack...")
    val logConsumer = Slf4jLogConsumer(log).withPrefix("localstack")
    val localStackContainer: LocalStackContainer =
      LocalStackContainer(DockerImageName.parse("localstack/localstack").withTag("0.11.2"))
        .withServices(
          LocalStackContainer.Service.SQS,
          LocalStackContainer.Service.SNS
        )
        .withClasspathResourceMapping(
          "/localstack/setup-sqs.sh",
          "/docker-entrypoint-initaws.d/setup-sqs.sh",
          BindMode.READ_WRITE
        )
        .withEnv("HOSTNAME_EXTERNAL", "localhost")
        .withEnv("DEFAULT_REGION", "eu-west-2")
        .waitingFor(
          Wait.forLogMessage(".*All Ready.*", 1)
        )

    log.info("Started localstack.")

    localStackContainer.start()
    localStackContainer.followOutput(logConsumer)
    return localStackContainer
  }

//  @Bean
//  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
//  fun queueUrl(
//    @Autowired awsSqsClient: AmazonSQS,
//    @Value("\${offender-events.sqs-queue-name}") queueName: String,
//    @Value("\${offender-events.sqs-dlq-name}") dlqName: String
//  ): String {
//    println("---------------------------")
//    println(awsSqsClient.getQueueUrl(queueName).queueUrl)
//    val result = awsSqsClient.createQueue(CreateQueueRequest("http://localhost:4576/queue/" + dlqName))
//    val dlqArn = awsSqsClient.getQueueAttributes(result.queueUrl, listOf(QueueAttributeName.QueueArn.toString()))
//    awsSqsClient.createQueue(
//      CreateQueueRequest(queueName).withAttributes(
//        mapOf(
//          QueueAttributeName.RedrivePolicy.toString() to
//            """{"deadLetterTargetArn":"${dlqArn.attributes["QueueArn"]}","maxReceiveCount":"5"}"""
//        )
//      )
//    )
//    return awsSqsClient.getQueueUrl(queueName).queueUrl
//  }
}
