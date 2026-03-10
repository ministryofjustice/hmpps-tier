package uk.gov.justice.digital.hmpps.hmppstier.model

interface TierCounts {
    val protectLevel: String
    val changeLevel: Int
    val count: Int
}