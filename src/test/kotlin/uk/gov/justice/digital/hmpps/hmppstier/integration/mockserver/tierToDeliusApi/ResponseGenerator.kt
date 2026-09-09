package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi

import uk.gov.justice.digital.hmpps.hmppstier.client.delius.*
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
        startDate = null,
        terminationDate = terminationDate,
        latestReleaseDate = null,
        isCustodial = false,
        sentenceTypeCode = sentenceCode,
        requirements = requirements,
        mainOffence = DeliusOffence(code = "00100", description = "Test offence", sentencingAct2026Exclusion = false),
        additionalOffences = emptyList(),
    )

    fun deliusRequirement(
        mainTypeCode: String,
        restrictive: Boolean,
    ) = DeliusRequirement(
        mainCategoryTypeCode = mainTypeCode,
        restrictive = restrictive
    )
}