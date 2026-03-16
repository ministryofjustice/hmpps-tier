package uk.gov.justice.digital.hmpps.hmppstier.exception

import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.StandardCharsets

class CrnNotFoundException(
    val crn: String,
    cause: WebClientResponseException
) : WebClientResponseException(
    cause.message,
    cause.statusCode.value(),
    cause.statusText,
    cause.headers,
    cause.responseBodyAsByteArray,
    StandardCharsets.UTF_8,
    cause.request
)