package commonskafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import commonskafka.CustomKafkaTemplate
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class KafkaProducerFactory(
    private val bootstrapServers: String,
    private val dlBootstrapServers: String,
    private val objectMapper: ObjectMapper,
    private val appId: String? = null,
) {

    private fun producerConfigs(bootstrapServers: String): MutableMap<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.BATCH_SIZE_CONFIG] = 16384
        props[ProducerConfig.LINGER_MS_CONFIG] = 1
        props[ProducerConfig.SEND_BUFFER_CONFIG] = 128 * 1024
        props[ProducerConfig.MAX_REQUEST_SIZE_CONFIG] = 1 * 1024 * 1024
        props[ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG] = 50
        props[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 60 * 1000
        props[ProducerConfig.BUFFER_MEMORY_CONFIG] = 32 * 1024 * 1024
        props[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "none"
        props[ProducerConfig.RETRIES_CONFIG] = 3
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 30 * 1000
        props[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = ProducerInterceptor::class.jvmName
        return props
    }

    fun <T : Any> createTemplate(clazz: KClass<T>): CustomKafkaTemplate<String?, T> {
        return createTemplate(clazz, null)
    }

    private fun <T : Any> createTemplate(
        clazz: KClass<T>,
        customConfig: Map<String, Any>?,
    ): CustomKafkaTemplate<String?, T> {
        val kafkaTemplate = createKafkaTemplate(clazz, customConfig, bootstrapServers)
        val dlKafkaTemplate = createKafkaTemplate(clazz, customConfig, dlBootstrapServers)
        return CustomKafkaTemplate(kafkaTemplate, dlKafkaTemplate, appId)
    }

    private fun <T : Any> createKafkaTemplate(
        clazz: KClass<T>?,
        customConfig: Map<String, Any>?,
        bootstrapServers: String,
    ): KafkaTemplate<String?, T> {
        val config = producerConfigs(bootstrapServers)
        customConfig?.entries?.forEach {
            config[it.key] = it.value
        }

        val jsonSerializer = JsonSerializer<T>(objectMapper)
        jsonSerializer.isAddTypeInfo = false // 객체의 타입 정보를 JSON 출력에 포함시킬지 여부를 결정

        val factory = DefaultKafkaProducerFactory<String?, T>(
            config,
            StringSerializer(),
            jsonSerializer
        )
//        customizers.orderedStream().forEach { it.customize(factory) }

        factory.setPhysicalCloseTimeout(30)
        return KafkaTemplate(factory)
    }
}

