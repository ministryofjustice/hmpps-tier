package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException

class SuccessUpdaterTest {

    @Test
    fun `Missing Topic exception thrown when calculation complete topic is not configured`() {
        val hmppsQueueService = mockk<HmppsQueueService>()

        every { hmppsQueueService.findByTopicId("hmppscalculationcompletetopic") } returns null

        Assertions.assertThrows(MissingTopicException::class.java) {
            SuccessUpdater(hmppsQueueService, mockk(), "")
        }
    }
}
