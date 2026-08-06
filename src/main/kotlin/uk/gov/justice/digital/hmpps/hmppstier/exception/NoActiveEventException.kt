package uk.gov.justice.digital.hmpps.hmppstier.exception

class NoActiveEventException(val crn: String) : RuntimeException("$crn has no active events")