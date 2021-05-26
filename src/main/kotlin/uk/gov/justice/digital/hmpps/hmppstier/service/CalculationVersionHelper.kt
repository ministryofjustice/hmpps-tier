package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CalculationVersionHelper(
  @Value("\${calculation.version}") private var calcVersion: Int
) {
  var calculationVersion: Int = calcVersion
}
