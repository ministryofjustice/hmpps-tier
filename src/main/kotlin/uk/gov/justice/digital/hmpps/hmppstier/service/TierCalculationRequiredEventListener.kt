package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.cloud.aws.messaging.listener.Acknowledgment
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Service

@Service
class TierCalculationRequiredEventListener {

  @SqsListener(value = arrayOf("test-queue"), deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  fun listener(msg: String, acknowledgment: Acknowledgment) {
    System.out.println("message: " + msg)
    acknowledgment.acknowledge()
  }
}
