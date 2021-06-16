package uk.gov.justice.digital.hmpps.hmppstier.service.debt

import com.opencsv.bean.CsvBindByPosition
import com.opencsv.bean.CsvToBean
import com.opencsv.bean.CsvToBeanBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime

@Service
class TierOverrideService(
  private val tierCalculationRepository: TierCalculationRepository
) {

  fun uploadCsvFile(file: MultipartFile) {
    throwIfFileEmpty(file)
    var fileReader: BufferedReader? = null
    try {
      fileReader = BufferedReader(InputStreamReader(file.inputStream))
      log.info("Starting converting from CSV")
      val requests = createCSVToBean(fileReader).parse()
      log.info("Finished converting from CSV ${requests.size} entries")
      sendMessagesFromList(requests)
      log.info("Finished saving overrides")
    } catch (ex: Exception) {
      println(ex)
      log.error(ex.localizedMessage)
      throw CsvImportException("Error during csv import")
    } finally {
      closeFileReader(fileReader)
    }
  }

  private fun sendMessagesFromList(requests: Collection<ActiveCrn>) {
    requests
      .chunked(10).forEach { messageRequests ->
        messageRequests.forEach { req ->
          log.info("Saving CRN, ${req.crn}")
          tierCalculationRepository.save(
            TierCalculationEntity(
              crn = req.crn!!,
              created = LocalDateTime.now(),
              data = TierCalculationResultEntity(
                protect = TierLevel(ProtectLevel.valueOf(req.score!!.uppercase().substring(0, 1)), 0, mapOf()),
                change = TierLevel(changeLevel(req), 0, mapOf()),
                "0"
              )
            )
          )
        }
        log.info("Sent Batch")
      }
  }

  private fun changeLevel(req: ActiveCrn) =
    ChangeLevel.values().first{it.value == req.score!!.substring(1, 2).toInt()}

  private fun throwIfFileEmpty(file: MultipartFile) {
    if (file.isEmpty)
      throw BadRequestException("Empty file")
  }

  private fun createCSVToBean(fileReader: BufferedReader?): CsvToBean<ActiveCrn> =
    CsvToBeanBuilder<ActiveCrn>(fileReader)
      .withType(ActiveCrn::class.java)
      .withSkipLines(1)
      .withIgnoreLeadingWhiteSpace(true)
      .build()

  private fun closeFileReader(fileReader: BufferedReader?) {
    try {
      fileReader!!.close()
    } catch (ex: IOException) {
      throw CsvImportException("Error closing file reader")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ActiveCrn(
  @CsvBindByPosition(position = 0)
  var crn: String? = null,

  @CsvBindByPosition(position = 1)
  var score: String? = null

)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(msg: String) : RuntimeException(msg)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class CsvImportException(msg: String) : RuntimeException(msg)
