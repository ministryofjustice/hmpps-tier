package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.UUID

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
)
