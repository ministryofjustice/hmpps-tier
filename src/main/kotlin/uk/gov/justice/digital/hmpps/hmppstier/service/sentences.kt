import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence

fun isCustodial(sentence: Sentence): Boolean =
  sentence.sentenceType.code in arrayOf("NC", "SC")
