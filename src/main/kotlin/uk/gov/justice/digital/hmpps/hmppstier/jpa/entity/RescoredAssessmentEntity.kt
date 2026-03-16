package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "ogrs4_rescored_assessment")
data class RescoredAssessmentEntity(
    @Id
    val id: Long,
    val crn: String,
    val completedDate: LocalDate,
    val arpScore: Double,
    val arpIsDynamic: Boolean,
    val arpBand: String,
    val csrpScore: Double,
    val csrpIsDynamic: Boolean,
    val csrpBand: String,
    val dcSrpScore: Double,
    val dcSrpBand: String,
    val iicSrpScore: Double,
    val iicSrpBand: String,
)