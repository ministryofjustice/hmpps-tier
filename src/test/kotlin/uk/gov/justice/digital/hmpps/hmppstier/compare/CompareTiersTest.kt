package uk.gov.justice.digital.hmpps.hmppstier.compare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompareTiersTest {

  @Test
  fun getsListOfDeliusTiers() {
    val deliusTiers = CompareTiers().loadDeliusTiers("src/test/resources/compare-tiers/test/")

    val expectedTiers = Tiers(
      listOf(
        Tier("X123400", "C0"),
        Tier("X123401", "A2"),
        Tier("X123402", "A0"),
        Tier("X123403", "B2"),
        Tier("X123404", "B0"),
        Tier("X123405", "C2"),
        Tier("X123406", "D2"),
        Tier("X123407", "D0"),
        Tier("X123408", "D1"),
        Tier("X123409", "D3"),
        Tier("X123410", "C1"),
        Tier("X123411", "C3"),
        Tier("X123412", "B1"),
        Tier("X123413", "B3"),
        Tier("X123414", "A1"),
        Tier("X123415", "A3"),
        Tier("X123499", "A3")
      )
    )
    assertEquals(deliusTiers, expectedTiers)
  }

  @Test
  fun getsListOfUTMTiers() {
    val utmTiers = CompareTiers().loadUtmTiers("src/test/resources/compare-tiers/test/")

    val expectedTiers = Tiers(
      listOf(
        Tier("X123400", "A2"),
        Tier("X123401", "C0"),
        Tier("X123402", "A0"),
        Tier("X123403", "B2"),
        Tier("X123404", "B0"),
        Tier("X123405", "C2"),
        Tier("X123406", "D2"),
        Tier("X123407", "D0"),
        Tier("X123408", "D1"),
        Tier("X123409", "D3"),
        Tier("X123410", "C1"),
        Tier("X123411", "C3"),
        Tier("X123412", "B1"),
        Tier("X123413", "B3"),
        Tier("X123414", "A1"),
        Tier("X123415", "A3")
      )
    )
    assertEquals(utmTiers, expectedTiers)
  }

  @Test
  fun outputsTierDifferences() {
    val tierDiffs = CompareTiers().compare("src/test/resources/compare-tiers/test/")
    assertEquals(TierDiffs(listOf(TierDiff("X123400", "C0", "A2"), TierDiff("X123401", "A2", "C0"), TierDiff("X123499", "A3", null))), tierDiffs)
  }
}
