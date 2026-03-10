package uk.gov.justice.digital.hmpps.hmppstier.cronjob

import org.springframework.boot.SpringApplication.exit
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationService
import kotlin.system.exitProcess

@Component
@ConditionalOnProperty("full-recalculation.enabled")
class FullRecalculationRunner(
    private val service: RecalculationService,
    private val applicationContext: ApplicationContext
) : ApplicationListener<ApplicationStartedEvent> {
    override fun onApplicationEvent(applicationStartedEvent: ApplicationStartedEvent) {
        service.recalculateAll()
        exitProcess(exit(applicationContext, { 0 }))
    }
}