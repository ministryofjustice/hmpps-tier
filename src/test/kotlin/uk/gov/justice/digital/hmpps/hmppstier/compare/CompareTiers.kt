package uk.gov.justice.digital.hmpps.hmppstier.compare

import com.opencsv.bean.CsvToBeanBuilder
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.InputStreamReader

class CompareTiers {

  fun loadDeliusTiers(path: String): Tiers {
    val deliusTiersFile = File(path + "wmt_ps.xlsx")

    val xlWb = WorkbookFactory.create(deliusTiersFile)

    // Row index specifies the row in the worksheet (starting at 0):
    // Cell index specifies the column within the chosen row (starting at 0):
    val crnColumnNumber = 1
    val tierColumnNumber = 2

    val xlWs = xlWb.getSheetAt(Flag_Warr_4_N)
    val tiers: MutableList<Tier> = mutableListOf()

    xlWs.iterator().forEach {
      val tierCell = it.getCell(tierColumnNumber).stringCellValue
      if (tierCell != "Tier_Code" && tierCell.isNotEmpty()) {
        tiers.add(
          Tier(
            it.getCell(crnColumnNumber).stringCellValue,
            deliusTierFrom(tierCell)
          )
        )
      }
    }

    return Tiers(tiers)
  }

  fun loadUtmTiers(path: String): Tiers {
    val utmTiersFile = File(path + "utm.csv")
    val reader = InputStreamReader(utmTiersFile.inputStream())

    try {
      val cb = CsvToBeanBuilder<UtmTier>(reader)
        .withType(UtmTier::class.java)
        .build()

      val tiers = cb.parse()
      reader.close()

      val utmTiers = tiers.map { Tier(it.crn!!, utmTierFrom(it)) }
      return Tiers(utmTiers)
    } catch (e: NullPointerException) {
      println("Check instructions in the README for removing the BOM from utm.csv")
      throw e
    }
  }

  private fun utmTierFrom(it: UtmTier) = it.protect.plus(UtmTierConverter[it.change])

  private fun deliusTierFrom(deliusTier: String) =
    DeliusTierConverter.getOrDefault(deliusTier, "No tier converted for $deliusTier")
}

data class UtmTier(
  var crn: String? = null,
  var protect: String? = null,
  var change: String? = null
)

fun main() {
  CompareTiers().loadDeliusTiers("src/test/resources/compare-tiers/delius/")
}

val DeliusTierConverter = mapOf(
  "1" to "A3", "2" to "A2", "3" to "A1", "4" to "A0",
  "5" to "B3", "6" to "B2", "7" to "B1", "8" to "B0",
  "9" to "C3", "10" to "C2", "11" to "C1", "12" to "C0",
  "13" to "D3", "14" to "D2", "15" to "D1", "16" to "D0",
)

val UtmTierConverter = mapOf("ZERO" to "0", "ONE" to "1", "TWO" to "2", "THREE" to "3")

data class Tiers(val tiers: List<Tier>)
data class Tier(val crn: String, val tier: String)
private const val Flag_Warr_4_N = 4
