package uk.gov.justice.digital.hmpps.hmppstier

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class WritebackDisabledInProduction {

  @Test
  fun isDisabled() {
    val prodConfig = Files.readString(Paths.get("helm_deploy/values-prod.yaml"))
    assertThat(prodConfig.contains("FLAGS_ENABLEDELIUSTIERUPDATES: false")).isTrue()
  }
}
