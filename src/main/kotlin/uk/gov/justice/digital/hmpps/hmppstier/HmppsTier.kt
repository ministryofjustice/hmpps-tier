package uk.gov.justice.digital.hmpps.hmppstier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication()
@EnableCaching
class HmppsTier

fun main(args: Array<String>) {
  runApplication<HmppsTier>(*args)
}
