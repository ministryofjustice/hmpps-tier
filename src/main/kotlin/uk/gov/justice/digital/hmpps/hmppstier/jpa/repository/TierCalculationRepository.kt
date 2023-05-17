package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import java.util.UUID

@Repository
interface TierCalculationRepository : CrudRepository<TierCalculationEntity, Long> {

  fun findFirstByCrnOrderByCreatedDesc(crn: String): TierCalculationEntity?

  fun findByCrnAndUuid(crn: String, calculationId: UUID): TierCalculationEntity?

  @Query(nativeQuery = true, value = "SELECT DISTINCT crn FROM tier_calculation ORDER BY crn DESC OFFSET ?1 LIMIT ?2")
  fun findDistinctCrn(
    @Param("offset") offset: Int,
    @Param("limit") limit: Int,
  ): List<String>
}
