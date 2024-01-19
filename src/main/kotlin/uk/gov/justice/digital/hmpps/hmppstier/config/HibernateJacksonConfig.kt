package uk.gov.justice.digital.hmpps.hmppstier.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class HibernateJacksonConfig(val objectMapper: ObjectMapper) {
    @PostConstruct
    fun setHibernateObjectMapper() {
        ObjectMapperWrapper.INSTANCE.objectMapper = objectMapper
    }
}