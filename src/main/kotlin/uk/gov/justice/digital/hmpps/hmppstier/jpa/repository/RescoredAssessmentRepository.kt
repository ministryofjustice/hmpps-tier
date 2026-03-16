package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.RescoredAssessmentEntity
import java.time.LocalDate

@Repository
interface RescoredAssessmentRepository : JpaRepository<RescoredAssessmentEntity, Long> {
    fun findByCrnAndCompletedDateAfter(
        crn: String,
        completedDate: LocalDate = LocalDate.now().minusWeeks(55)
    ): RescoredAssessmentEntity?
}
