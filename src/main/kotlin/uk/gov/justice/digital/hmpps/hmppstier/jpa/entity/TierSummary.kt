package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import jakarta.persistence.*
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.*

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "tier_summary")
class TierSummary(

    @Id
    val crn: String,

    var uuid: UUID,
    @Column(name = "protect_level")
    var protectLevel: String,
    @Column(name = "change_level")
    var changeLevel: Int,

    @Version
    val version: Long = 0,

    @LastModifiedDate
    val lastModified: LocalDateTime = LocalDateTime.now(),
)

interface TierSummaryRepository : JpaRepository<TierSummary, String> {
    @Query(
        """
    select ts.protectLevel, ts.changeLevel, count(1) 
    from TierSummary ts
    group by ts.protectLevel, ts.changeLevel
    order by ts.protectLevel, ts.changeLevel
  """
    )
    fun getTierCounts(): List<TierCounts>
}

interface TierCounts {
    val protectLevel: String
    val changeLevel: Int
    val count: Int
}