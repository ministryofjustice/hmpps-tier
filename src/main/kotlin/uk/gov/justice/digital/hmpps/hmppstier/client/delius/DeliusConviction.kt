package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class DeliusConviction @JsonCreator constructor(
    val startDate: LocalDate?,
    val terminationDate: LocalDate?,
    val latestReleaseDate: LocalDate?,
    val isCustodial: Boolean,
    val sentenceTypeCode: String,
    val requirements: List<DeliusRequirement>,
    val mainOffence: DeliusOffence,
    val additionalOffences: List<DeliusOffence>,
)