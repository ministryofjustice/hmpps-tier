package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.amazonaws.services.sqs.AmazonSQSAsync
import java.nio.file.Files
import java.nio.file.Paths

fun putMessageOnQueue(client: AmazonSQSAsync, queueUrl: String, crn: String) {
  val message = calculationMessage(crn)
  client.sendMessage(queueUrl, message)
}

fun getNumberOfMessagesCurrentlyOnQueue(client: AmazonSQSAsync, queueUrl: String): Int? {
  val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
  return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
}

private fun calculationMessage(crn: String): String {
  return Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    .replace("X373878", crn)
}
