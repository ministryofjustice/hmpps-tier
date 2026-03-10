package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class DeliusConviction @JsonCreator constructor(
    val terminationDate: LocalDate?,
    val sentenceTypeCode: String,
    val requirements: List<DeliusRequirement>,
)