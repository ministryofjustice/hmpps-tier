package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import java.util.*

@Repository
interface TierCalculationRepository : JpaRepository<TierCalculationEntity, Long> {

  fun findFirstByCrnOrderByCreatedDesc(crn: String): TierCalculationEntity?

  fun findByCrnAndUuid(crn: String, calculationId: UUID): TierCalculationEntity?

  fun deleteAllByCrn(crn: String)
}
