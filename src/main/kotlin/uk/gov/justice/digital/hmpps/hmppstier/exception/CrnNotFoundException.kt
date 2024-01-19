package uk.gov.justice.digital.hmpps.hmppstier.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class CrnNotFoundException(message: String) : HttpClientErrorException(HttpStatus.NOT_FOUND, message)