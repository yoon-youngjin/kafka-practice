package commonskafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import commonskafka.CustomKafkaTemplate
import commonskafka.producer.KafkaProducerConfiguration
import commonskafka.producer.KafkaProducerFactory
import mu.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger { }

@Configuration
@ConditionalOnProperty(name = ["kafka.consume.enable"], havingValue = "true")
@AutoConfigureAfter(KafkaProducerConfiguration::class)
class KafkaConsumerConfiguration {

    @Bean
    fun kafkaConsumerFactory(producerFactory: KafkaProducerFactory): KafkaConsumerFactory {
        return KafkaConsumerFactory(producerFactory)
    }

    @Bean
    fun cdlKafkaTemplate(producerFactory: KafkaProducerFactory): CustomKafkaTemplate<String?, Any> {
        return producerFactory.createTemplate(Any::class)
    }
}

