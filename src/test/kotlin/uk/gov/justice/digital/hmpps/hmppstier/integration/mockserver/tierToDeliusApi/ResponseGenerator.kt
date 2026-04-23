package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi

import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRequirement
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusResponse
import java.time.LocalDate

object ResponseGenerator {
    fun deliusResponse(
        gender: String = "Male",
        ogrsScore: String? = "21",
        rsrScore: String? = "23",
        registrations: List<DeliusRegistration> = listOf(),
        convictions: List<DeliusConviction> = listOf(),
        previousEnforcementActivity: Boolean = false,
        latestReleaseDate: LocalDate? = LocalDate.of(2022, 1, 1),
        hasActiveEvent: Boolean = true,
    ) = DeliusResponse(
        gender = gender,
        registrations = registrations,
        convictions = convictions,
        rsrscore = rsrScore?.toBigDecimal(),
        ogrsscore = ogrsScore?.toInt(),
        previousEnforcementActivity = previousEnforcementActivity,
        latestReleaseDate = latestReleaseDate,
        hasActiveEvent = hasActiveEvent,
    )

    fun deliusRegistration(
        level: String? = null,
        category: String? = null,
        typeCode: String = "MAPP",
        date: LocalDate = LocalDate.of(2021, 2, 1)
    ) = DeliusRegistration(
        code = typeCode,
        level = level,
        category = category,
        date = date
    )

    fun deliusConviction(
        requirements: List<DeliusRequirement> = listOf(),
        sentenceCode: String = "NC",
        terminationDate: LocalDate? = null,
    ) = DeliusConviction(
        terminationDate = terminationDate,
        sentenceTypeCode = sentenceCode,
        requirements = requirements,
    )

    fun deliusRequirement(
        mainTypeCode: String,
        restrictive: Boolean,
    ) = DeliusRequirement(
        mainCategoryTypeCode = mainTypeCode,
        restrictive = restrictive
    )
}