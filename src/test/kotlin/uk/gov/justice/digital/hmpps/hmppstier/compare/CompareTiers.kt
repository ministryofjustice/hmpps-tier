package uk.gov.justice.digital.hmpps.hmppstier.compare

import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

class CompareTiers {
  fun loadDeliusTiers() {
    val file = File("src/test/resources/compare-tiers/delius/wmt_ps.xlsx")

    // Instantiate Excel workbook using existing file:
    var xlWb = WorkbookFactory.create(file)

    // Row index specifies the row in the worksheet (starting at 0):
    val rowNumber = 1
    // Cell index specifies the column within the chosen row (starting at 0):
    val crnColumnNumber = 1
    val tierColumnNumber = 2

    // Get reference to first sheet:
    val xlWs = xlWb.getSheetAt(Flag_Warr_4_N)
    println(xlWs.getRow(rowNumber).getCell(crnColumnNumber))
    println(xlWs.getRow(rowNumber).getCell(tierColumnNumber))
  }
}

fun main() {
  println("Main method called")
  CompareTiers().loadDeliusTiers()
}

private const val Flag_Warr_4_N = 4
