package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CalculationVersionHelper(
  @Value("\${calculation.version}") private var calcVersion: Int
) {
  var calculationVersion: Int = calcVersion

  fun arsonToggleEnabled(): Boolean = versionAtLeast(3)

  fun sentenceToggleEnabled(): Boolean = versionAtLeast(3)

  fun tierAThresholdFixEnabled(): Boolean = versionAtLeast(2)

  private fun versionAtLeast(value: Int): Boolean = calculationVersion >= value
}
