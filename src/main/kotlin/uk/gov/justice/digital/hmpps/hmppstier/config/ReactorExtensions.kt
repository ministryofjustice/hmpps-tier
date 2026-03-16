package uk.gov.justice.digital.hmpps.hmppstier.config

import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry

object ReactorExtensions {
    fun <T> Retry.invoke(function: () -> T): T = Mono.fromCallable(function)
        .subscribeOn(Schedulers.boundedElastic())
        .retryWhen(this)
        .block()!!
}
