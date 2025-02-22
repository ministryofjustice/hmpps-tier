package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "tier_calculation")
data class TierCalculationEntity(

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column
    val uuid: UUID = UUID.randomUUID(),

    @Column
    val crn: String,

    @Column
    val created: LocalDateTime,

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    val data: TierCalculationResultEntity,

    @Column
    val changeReason: String? = null,
) {
    fun protectLevel() = data.protect.tier.value
    fun changeLevel() = data.change.tier.value
}
