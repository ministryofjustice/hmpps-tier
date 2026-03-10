package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator

data class DeliusRequirement @JsonCreator constructor(
    val mainCategoryTypeCode: String,
    val restrictive: Boolean,
)