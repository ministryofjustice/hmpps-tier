package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryEntity
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
    fun getTierV2Counts(): List<TierCounts>

    @Query(
        """
        select ts.tier as tier, count(ts.crn) as count
        from TierSummaryEntity ts
        where ts.tier is not null
        group by ts.tier
        order by ts.tier
        """
    )
    fun getTierV3Counts(): List<Pair<String, Int>>
}