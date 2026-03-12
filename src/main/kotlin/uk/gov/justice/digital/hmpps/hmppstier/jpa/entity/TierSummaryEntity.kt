package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import jakarta.persistence.*
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "tier_summary")
class TierSummaryEntity(
    @Id
    val crn: String,

    @Column
    var uuid: UUID,

    @Column(columnDefinition = "char(1)")
    var tier: String?, // can make this non-nullable once all cases have been recalculated

    @Column
    var protectLevel: String,

    @Column
    var changeLevel: Int,

    @Column
    var unsupervised: Boolean,

    @Version
    val version: Long = 0,

    @LastModifiedDate
    val lastModified: LocalDateTime = LocalDateTime.now(),
)
