package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import java.io.Serializable
import java.time.LocalDateTime

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "outputVersion",
    visible = true,
    defaultImpl = OGRS3PRedictors::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = OGRS4Predictors::class, name = "2"),
)
interface AllPredictorVersioned<out T> {
    val completedDate: LocalDateTime?
    val assessmentType: AssessmentType?
    val outputVersion: String
    val output: T?
}

data class OGRS4Predictors(
    override val completedDate: LocalDateTime,
    override val assessmentType: AssessmentType? = null,
    override val outputVersion: String = "2",
    override val output: AllPredictorDto? = null,
) : AllPredictorVersioned<AllPredictorDto>, Serializable

data class OGRS3PRedictors(
    override val completedDate: LocalDateTime? = null,
    override val assessmentType: AssessmentType? = null,
    override val outputVersion: String = "1",
    override val output: Any? = null,
) : AllPredictorVersioned<Any>, Serializable
