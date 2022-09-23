package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import java.nio.file.Files
import java.nio.file.Paths

fun putMessageOnQueue(client: AmazonSQSAsync, queueUrl: String, crn: String) {
  val message = calculationMessage(crn)
  client.sendMessage(queueUrl, message)
}

fun noMessagesCurrentlyOnQueue(client: AmazonSQSAsync, queueUrl: String) {
  await untilCallTo {
    getNumberOfMessagesCurrentlyOnQueue(
      client,
      queueUrl
    )
  } matches { it == 0 }
}

fun oneMessageCurrentlyOnQueue(client: AmazonSQSAsync, queueUrl: String) {
  await untilCallTo {
    getNumberOfMessagesCurrentlyOnQueue(
      client,
      queueUrl
    )
  } matches { it == 1 }
}

fun oneMessageCurrentlyOnDeadletterQueue(client: AmazonSQS, queueUrl: String) {
  await untilCallTo {
    getNumberOfMessagesCurrentlyOnDeadLetterQueue(
      client,
      queueUrl
    )
  } matches { it == 1 }
}

private fun getNumberOfMessagesCurrentlyOnQueue(client: AmazonSQSAsync, queueUrl: String): Int? {
  val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
  return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
}

private fun getNumberOfMessagesCurrentlyOnDeadLetterQueue(client: AmazonSQS, queueUrl: String): Int? {
  val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
  return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
}

private fun calculationMessage(crn: String): String {
  return Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    .replace("X373878", crn)
}
