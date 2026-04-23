package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import java.util.*

data class DomainEvent(
    val eventType: String,
    val description: String,
    val personReference: PersonReference,
    val additionalInformation: Map<String, Any?>? = mapOf(),
) {
    val crn = personReference.identifiers.firstOrNull { it.type == "CRN" }?.value
    val sourceCrn = additionalInformation?.get("sourceCRN") as String?
    val targetCrn = additionalInformation?.get("targetCRN") as String?
    val unmergedCrn = additionalInformation?.get("unmergedCRN") as String?
    val reactivatedCrn = additionalInformation?.get("reactivatedCRN") as String?
    val recalculationSource = additionalInformation?.get("recalculationSource") as String?

    fun changeReason(): String =
        when (eventType) {
            "enforcement.breach.concluded" -> "A breach was concluded"
            "enforcement.breach.raised" -> "A breach was raised"
            "enforcement.recall.concluded" -> "A recall to custody process was concluded"
            "enforcement.recall.raised" -> "A recall to custody process was started"
            "probation-case.engagement.created" -> "The case was created"
            "probation-case.merge.completed" -> "The case was merged from $sourceCrn into $targetCrn"
            "probation-case.unmerge.completed" -> "The case was un-merged from $unmergedCrn and $reactivatedCrn"
            "probation-case.registration.added" -> "${registrationOfType()} was added"
            "probation-case.registration.deleted" -> "${registrationOfType()} was removed"
            "probation-case.registration.deregistered" -> "${registrationOfType()} was removed"
            "probation-case.registration.updated" -> "${registrationOfType()} was updated"
            "probation-case.requirement.created" -> "${requirementOfType()} was added"
            "probation-case.requirement.deleted" -> "${requirementOfType()} was removed"
            "probation-case.requirement.terminated" -> "${requirementOfType()} was terminated"
            "probation-case.requirement.unterminated" -> "${requirementOfType()} was un-terminated"
            "probation-case.sentence.created" -> "A sentence was added"
            "probation-case.sentence.amended" -> "A sentence was amended"
            "probation-case.sentence.terminated" -> "A sentence was terminated"
            "probation-case.sentence.unterminated" -> "A sentence was un-terminated"
            "probation-case.sentence.deleted" -> "A sentence was removed"
            "probation-case.sentence.moved" -> "A sentence was moved"
            "conviction.changed" -> "The supervision status changed"
            "risk-assessment.scores.determined" -> "An OASys assessment was produced"

            else -> description
        }

    private fun registrationOfType() = "A registration" +
        (additionalInformation?.get("registerTypeDescription")?.let { " of type '$it'" } ?: "")

    private fun requirementOfType() = "A requirement" +
        (additionalInformation?.get("requirementMainType")?.let { " of type '$it'" } ?: "")

    data class PersonReference(val identifiers: List<Identifier>)
    data class Identifier(val type: String, val value: String)
    data class AdditionalInformation(val calculationId: UUID)
}