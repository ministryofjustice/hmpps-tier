package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.nio.file.Files
import java.nio.file.Paths

fun putMessageOnDomainQueue(client: SqsAsyncClient, queueUrl: String, crn: String) {
    val message = calculationDomainMessage(crn)
    client.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build()).get()
}

fun putRecallMessageOnDomainQueue(client: SqsAsyncClient, queueUrl: String, crn: String) {
    val message = calculationRecallDomainMessage(crn)
    client.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build()).get()
}

fun oneMessageCurrentlyOnQueue(client: SqsAsyncClient, queueUrl: String) {
    await untilCallTo {
        client.countMessagesOnQueue(queueUrl).get()
    } matches { it == 1 }
}

private fun calculationDomainMessage(crn: String): String {
    return Files.readString(Paths.get("src/test/resources/fixtures/sqs/domain-calculation-event.json"))
        .replace("X373878", crn)
}

private fun calculationRecallDomainMessage(crn: String): String {
    return Files.readString(Paths.get("src/test/resources/fixtures/sqs/recall-domain-calculation-event.json"))
        .replace("X373878", crn)
}
