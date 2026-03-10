package uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.entity.TierSummaryEntity
import uk.gov.justice.digital.hmpps.hmppstier.model.TierCounts

interface TierSummaryRepository : JpaRepository<TierSummaryEntity, String> {
    @Query(
        """
        select ts.protectLevel as protectLevel, ts.changeLevel as changeLevel, count(ts.crn) as count
        from TierSummaryEntity ts
        group by ts.protectLevel, ts.changeLevel
        order by ts.protectLevel, ts.changeLevel
        """
    )
    fun getTierCounts(): List<TierCounts>
}