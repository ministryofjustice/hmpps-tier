package uk.gov.justice.digital.hmpps.hmppstier.test

import java.util.concurrent.atomic.AtomicInteger

object TestData {
    private val LETTER = ('A'..'Z').random()
    private val CRN_SEQUENCE = AtomicInteger(0)
    fun crn() = LETTER + CRN_SEQUENCE.getAndIncrement().toString().padStart(6, '0')
}