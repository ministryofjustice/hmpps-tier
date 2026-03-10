package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import java.io.Serializable

data class Threshold(val standard: Int, val severe: Int) : Serializable