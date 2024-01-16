package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun objectMapper() = jacksonObjectMapper().registerModules(JavaTimeModule())