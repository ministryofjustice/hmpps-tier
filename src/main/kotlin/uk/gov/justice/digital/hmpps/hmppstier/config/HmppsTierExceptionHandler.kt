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
import uk.gov.justice.digital.hmpps.hmppstier.dto.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException

@RestControllerAdvice
class HmppsTierExceptionHandler {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("EntityNotFoundException: {}", e.message)
        return ResponseEntity
            .status(NOT_FOUND)
            .body(ErrorResponse(status = 404, developerMessage = e.message))
    }

    @ExceptionHandler(HttpMessageConversionException::class)
    fun handleHttpMessageConversionException(e: HttpMessageConversionException): ResponseEntity<ErrorResponse> {
        log.error("HttpMessageConversionException: {}", e.message)
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(ErrorResponse(status = 400, developerMessage = e.message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.error("IllegalArgumentException: {}", e.message)
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(ErrorResponse(status = 400, developerMessage = e.message))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        log.error("MethodArgumentTypeMismatchException: {}", e.message)
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(ErrorResponse(status = 400, developerMessage = e.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Exception", e)
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 500,
                    developerMessage = "Internal Server Error. Check Logs",
                    userMessage = "An unexpected error has occurred",
                ),
            )
    }
}
