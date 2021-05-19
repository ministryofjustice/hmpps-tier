package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence

fun Sentence.isCustodial(): Boolean =
  this.sentenceType in arrayOf("NC", "SC")
