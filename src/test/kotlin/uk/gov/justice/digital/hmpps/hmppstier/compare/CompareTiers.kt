package uk.gov.justice.digital.hmpps.hmppstier.compare

import com.opencsv.CSVWriter
import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.bean.StatefulBeanToCsvBuilder
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.time.LocalDateTime

class CompareTiers {

  fun loadDeliusTiers(path: String): Tiers {
    val deliusTiersFile = File(path.plus("delius/wmt_ps.xlsx"))

    val xlWb = WorkbookFactory.create(deliusTiersFile)

    // Row index specifies the row in the worksheet (starting at 0):
    // Cell index specifies the column within the chosen row (starting at 0):
    val crnColumnNumber = 1
    val tierColumnNumber = 2

    val xlWs = xlWb.getSheetAt(Flag_Warr_4_N)
    val tiers: MutableList<Tier> = mutableListOf()

    xlWs.iterator().forEach {
      try {
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
      catch(e: NullPointerException){
        println("empty cell?")
      }
    }
    println("Finished reading delius tiers ${tiers.size} at ${LocalDateTime.now()}")
    return Tiers(tiers.sortedBy { it.crn })
  }

  fun loadUtmTiers(path: String): Tiers {
    val utmTiersFile = File(path.plus("utm/utm.csv"))
    val reader = InputStreamReader(utmTiersFile.inputStream())

    try {
      val cb = CsvToBeanBuilder<UtmTier>(reader)
        .withType(UtmTier::class.java)
        .build()

      val tiers = cb.parse()
      reader.close()

      val utmTiers = tiers.map { Tier(it.crn!!, utmTierFrom(it)) }
      println("Finished reading utm tiers ${utmTiers.size}  at ${LocalDateTime.now()}")
      return Tiers(utmTiers.sortedBy { it.crn })
    } catch (e: NullPointerException) {
      println("Check instructions in the README for removing the BOM from utm.csv")
      throw e
    }
  }

  private fun utmTierFrom(it: UtmTier) = it.protect.plus(UtmTierConverter[it.change])

  private fun deliusTierFrom(deliusTier: String) =
    DeliusTierConverter.getOrDefault(deliusTier, "No tier converted for $deliusTier")

  fun compare(path: String): TierDiffs {
    println("starting at ${LocalDateTime.now()}")
    val deliusTiers = loadDeliusTiers(path) // 10 seconds
    val utmTiers = loadUtmTiers(path) // 2 seconds
    val nonMatchingTiers = deliusTiers.tiers.filter { !utmTiers.matches(it) } // > 1 hour
    println("Finished getting list of non-matching tiers ${nonMatchingTiers.size}  at ${LocalDateTime.now()}")
    // should have written to disk here. There are 120 in all
    val deliusDiffs = nonMatchingTiers.map { TierDiff(it.crn, it.tier, utmTiers.find(it)?.tier) }

    return TierDiffs(deliusDiffs)

  }
}

data class UtmTier(
  var crn: String? = null,
  var protect: String? = null,
  var change: String? = null
)

val DeliusTierConverter = mapOf(
  "1" to "A3", "2" to "A2", "3" to "A1", "4" to "A0",
  "5" to "B3", "6" to "B2", "7" to "B1", "8" to "B0",
  "9" to "C3", "10" to "C2", "11" to "C1", "12" to "C0",
  "13" to "D3", "14" to "D2", "15" to "D1", "16" to "D0",
)

val UtmTierConverter = mapOf("ZERO" to "0", "ONE" to "1", "TWO" to "2", "THREE" to "3")

class Tiers() {
  lateinit var tiers: List<Tier>
  lateinit var tiersByCrn: Map<String, String>
  constructor(tiers: List<Tier>) : this() {
    this.tiers = tiers
    tiersByCrn = tiers.associate { it.crn to it.tier }
  }
  fun matches(tier: Tier): Boolean {
    return tiersByCrn[tier.crn].equals(tier.tier)
    //return tiers.contains(tier)
  }

  fun find(tier: Tier): Tier? {
    return tiers.find { it.crn == tier.crn }
  }

  override fun toString(): String {
    return tiers.toString()
  }

  override fun equals(other: Any?): Boolean {
    if(other is Tiers) {
      return tiers == other.tiers
    }
    return false
  }
}

data class Tier(val crn: String, val tier: String)

data class TierDiffs(val tierdiffs: List<TierDiff>)
data class TierDiff(val crn: String, val deliusTier: String, val utmTier: String?)

private const val Flag_Warr_4_N = 4

fun main() {
  val tierDiffs = CompareTiers().compare("src/test/resources/compare-tiers/")
  val csvWriter = CSVWriter(FileWriter( File("src/test/resources/compare-tiers/diffs.csv") ))

  StatefulBeanToCsvBuilder<TierDiff>(csvWriter)
    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
    .build()
    .write(tierDiffs.tierdiffs)

  csvWriter.close()
}
