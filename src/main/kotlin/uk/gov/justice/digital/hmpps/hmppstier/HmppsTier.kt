package uk.gov.justice.digital.hmpps.hmppstier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsTier

fun main(args: Array<String>) {
  runApplication<HmppsTier>(*args)
}
