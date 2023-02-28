package uk.gov.justice.digital.hmpps.hmppstier.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppstier.dto.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException

@RestControllerAdvice
class HmppsTierExceptionHandler {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): Mono<ResponseEntity<ErrorResponse>> {
    log.error("EntityNotFoundException: {}", e.message)
    return Mono.just(
      ResponseEntity
        .status(NOT_FOUND)
        .body(ErrorResponse(status = 404, developerMessage = e.message))
    )
  }

  @ExceptionHandler(HttpMessageConversionException::class)
  fun handleHttpMessageConversionException(e: HttpMessageConversionException): Mono<ResponseEntity<ErrorResponse>> {
    log.error("HttpMessageConversionException: {}", e.message)
    return Mono.just(
      ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(status = 400, developerMessage = e.message))
    )
  }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: IllegalArgumentException): Mono<ResponseEntity<ErrorResponse>> {
    log.error("IllegalArgumentException: {}", e.message)
    return Mono.just(
      ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(status = 400, developerMessage = e.message))
    )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): Mono<ResponseEntity<ErrorResponse>> {
    log.error("MethodArgumentTypeMismatchException: {}", e.message)
    return Mono.just(
      ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(status = 400, developerMessage = e.message))
    )
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): Mono<ResponseEntity<ErrorResponse>> {
    log.error("Exception", e)
    return Mono.just(
      ResponseEntity
        .status(BAD_REQUEST)
        .body(
          ErrorResponse(
            status = 500,
            developerMessage = "Internal Server Error. Check Logs",
            userMessage = "An unexpected error has occurred"
          )
        )
    )
  }
}
