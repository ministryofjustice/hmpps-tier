package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@TypeDefs(
  TypeDef(name = "json", typeClass = JsonStringType::class),
  TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
)
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

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  val data: TierCalculationResultEntity
) {
  companion object {
    fun from(crn: String, protectLevel: TierLevel<ProtectLevel>, changeLevel: TierLevel<ChangeLevel>, clock: Clock, version: String): TierCalculationEntity {
      return TierCalculationEntity(
        crn = crn,
        created = LocalDateTime.now(clock),
        data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel, calculationVersion = version)
      )
    }
  }
}
