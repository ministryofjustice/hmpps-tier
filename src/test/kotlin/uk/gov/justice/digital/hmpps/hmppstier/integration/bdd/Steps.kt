package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import io.cucumber.java8.En

class Steps : En {

  init {
    Given("an RSR score of {string}") { rsr: String ->
      println("Running a cucumber test")
      println("***********************")
    }
  }
}
