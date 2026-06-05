package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import java.math.BigDecimal

internal object ReoffendingPredictorTable {
    val CSRP_THRESHOLDS = arrayOf(6.9, 3.0, 1.0, 0.5, 0.0)
    val ARP_THRESHOLDS = arrayOf(90, 75, 50, 25, 15, 0)
    val ARP_CSRP_LOOKUP_TABLE = arrayOf(
        // CSRP  /  ARP = (90,75,50,25,15,0)
        /* 6.9+ */ arrayOf(A, A, B, B, B, B),
        /* 3.0+ */ arrayOf(A, B, C, C, C, C),
        /* 1.0+ */ arrayOf(B, C, D, E, E, E),
        /* 0.5+ */ arrayOf(C, D, E, E, F, F),
        /* 0.0+ */ arrayOf(D, D, E, F, F, G),
    )

    fun calculate(arp: BigDecimal, csrp: BigDecimal): Tier {
        val row = CSRP_THRESHOLDS.indexOfFirst { csrp >= it }
        val col = ARP_THRESHOLDS.indexOfFirst { arp >= it }
        return ARP_CSRP_LOOKUP_TABLE[row][col]
    }

    operator fun BigDecimal.compareTo(value: Int) = compareTo(value.toBigDecimal())
    operator fun BigDecimal.compareTo(value: Double) = compareTo(value.toBigDecimal())
}
