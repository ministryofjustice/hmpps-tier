package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.model.TierHistory
import java.util.*

@Repository
interface TierCalculationRepository : JpaRepository<TierCalculationEntity, Long> {
    fun findByCrnOrderByCreatedDesc(crn: String): List<TierCalculationEntity>

    @Query(
        """
        select uuid as "calculationId",
               created as "calculationDate",
               change_reason as "changeReason",
               data ->> 'tier' as tier,
               cast(data ->> 'provisional' as boolean) as provisional,
               data #>> '{protect,tier}' as "protectLevel",
               data #>> '{change,tier}' as "changeLevel",
               cast(data #>> '{deliusInputs,registrations,unsupervised}' as boolean) as unsupervised
        from tier_calculation
        where crn = :crn
        order by created desc
        """,
        nativeQuery = true,
    )
    fun findHistoryByCrn(@Param("crn") crn: String): List<TierHistory>

    fun findFirstByCrnOrderByCreatedDesc(crn: String): TierCalculationEntity?
    fun findByCrnAndUuid(crn: String, calculationId: UUID): TierCalculationEntity?
    fun deleteAllByCrn(crn: String)
}
