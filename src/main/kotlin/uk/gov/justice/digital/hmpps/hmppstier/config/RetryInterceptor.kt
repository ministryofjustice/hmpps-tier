package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClientException
import java.io.IOException
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class RetryInterceptor(private val retries: Int = 3, private val delay: Duration = Duration.ofMillis(350)) :
    ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse = retry(
        retries,
        listOf(
            RestClientException::class,
            IOException::class
        ),
        delay
    ) {
        execution.execute(request, body)
    }
}

fun <T> retry(
    maxRetries: Int,
    exceptions: List<KClass<out Exception>> = listOf(Exception::class),
    delay: Duration = Duration.ofMillis(200),
    code: () -> T
): T {
    var throwable: Throwable?
    (1..maxRetries).forEach { count ->
        try {
            return code()
        } catch (e: Throwable) {
            val matchedException = exceptions.firstOrNull { it.isInstance(e) }
            throwable = if (matchedException != null && count < maxRetries) {
                null
            } else {
                e
            }
            if (throwable == null) {
                TimeUnit.MILLISECONDS.sleep(delay.toMillis() * count * count)
            } else {
                throw throwable!!
            }
        }
    }
    throw RuntimeException("unknown error")
}