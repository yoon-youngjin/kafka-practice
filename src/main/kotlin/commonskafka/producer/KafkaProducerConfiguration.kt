package commonskafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import commonskafka.KafkaCommonConfiguration
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@AutoConfigureAfter(KafkaCommonConfiguration::class) // 해당 어노테이션으로 오토 컨피그의 순서를 지정할 수 있다.
class KafkaProducerConfiguration {
    @Bean
    @ConditionalOnBean(KafkaProducerFactory::class)
    fun kafkaProducerFactory(
        @Value("\${kafka.service.produce.bootstrap.servers}")
        bootstrapServers: String,
        @Value("\${kafka.dl.bootstrap.servers}")
        dlBootstrapServers: String,
        @Qualifier("kafkaObjectMapper")
        objectMapper: ObjectMapper,
        customizers: ObjectProvider<DefaultKafkaProducerFactoryCustomizer>,
        @Value("\${app.id}") appId: String?,
    ): KafkaProducerFactory {
        return KafkaProducerFactory(
            bootstrapServers = bootstrapServers,
            dlBootstrapServers = dlBootstrapServers,
            objectMapper = objectMapper,
            appId = appId,
        )

    }
}

