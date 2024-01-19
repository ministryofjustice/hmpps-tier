package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class AdditionalFactorForWomen(val section: String, val answerCode: String) {

    IMPULSIVITY("11", "11.2"),
    TEMPER_CONTROL("11", "11.4"),
    PARENTING_RESPONSIBILITIES("6", "6.9"),
    ;
}
