package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.RescoredAssessmentEntity

@Repository
interface RescoredAssessmentRepository : JpaRepository<RescoredAssessmentEntity, Long> {
    fun findFirstByCrnOrderByCompletedDateDesc(crn: String): RescoredAssessmentEntity?
}
