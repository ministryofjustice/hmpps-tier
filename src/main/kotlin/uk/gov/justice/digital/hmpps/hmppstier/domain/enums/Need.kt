package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class Need(val weighting: Int) {
  ACCOMMODATION(1),
  EDUCATION_TRAINING_AND_EMPLOYABILITY(1),
  RELATIONSHIPS(1),
  LIFESTYLE_AND_ASSOCIATES(1),
  DRUG_MISUSE(1),
  ALCOHOL_MISUSE(1),
  THINKING_AND_BEHAVIOUR(2),
  ATTITUDES(2)
}
