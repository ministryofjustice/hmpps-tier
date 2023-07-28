package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import java.util.UUID

@Repository
interface TierCalculationRepository : CrudRepository<TierCalculationEntity, Long> {

  fun findFirstByCrnOrderByCreatedDesc(crn: String): TierCalculationEntity?

  fun findByCrnAndUuid(crn: String, calculationId: UUID): TierCalculationEntity?
}
