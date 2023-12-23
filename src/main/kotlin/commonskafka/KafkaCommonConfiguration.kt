package commonskafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger { }

@Configuration
class KafkaCommonConfiguration {

    @Bean
    @ConditionalOnMissingBean(KafkaObjectMapper::class)
    fun kafkaObjectMapper(): KafkaObjectMapper {
        logger.info("kafkaObjectMapper init by commons-kafka")
        val defaultObjectMapper = jacksonObjectMapper()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)

        return KafkaObjectMapper(defaultObjectMapper)
    }
}