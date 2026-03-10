package uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.entity

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

    var uuid: UUID,
    @Column(name = "protect_level")
    var protectLevel: String,
    @Column(name = "change_level")
    var changeLevel: Int,

    var unsupervised: Boolean,

    @Version
    val version: Long = 0,

    @LastModifiedDate
    val lastModified: LocalDateTime = LocalDateTime.now(),
)
