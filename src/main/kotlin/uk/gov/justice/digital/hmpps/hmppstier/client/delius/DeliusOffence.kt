package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator

data class DeliusOffence @JsonCreator constructor(
    val code: String,
    val description: String,
    val sentencingAct2026Exclusion: Boolean,
)