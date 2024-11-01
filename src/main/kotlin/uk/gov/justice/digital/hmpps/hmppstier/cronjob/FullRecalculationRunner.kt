package uk.gov.justice.digital.hmpps.hmppstier.cronjob

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication.exit
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService
import kotlin.system.exitProcess

@Component
@ConditionalOnProperty("full-recalculation.enabled")
class FullRecalculationRunner(
    @Value("\${full-recalculation.dry-run:false}")
    private val dryRun: Boolean,
    private val service: TriggerCalculationService,
    private val applicationContext: ApplicationContext
) : ApplicationListener<ApplicationStartedEvent> {
    override fun onApplicationEvent(applicationStartedEvent: ApplicationStartedEvent) {
        service.recalculateAll(dryRun)
        exitProcess(exit(applicationContext, { 0 }))
    }
}