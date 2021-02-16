package uk.gov.justice.digital.hmpps.hmppstier.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
  val status: Int? = 0,

  val developerMessage: String? = null,

  val errorCode: Int? = null,

  val userMessage: String? = null,

  val moreInfo: String? = null
)
