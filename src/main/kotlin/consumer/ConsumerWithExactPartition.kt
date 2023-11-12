package consumer

import java.time.Duration
import java.util.Collections
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ConsumerWithExactPartition::class.java)

private const val TOPIC_NAME = "test"
private const val TOPIC_NUMBER = 0
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
private const val GROUP_ID = "test-group"

class ConsumerWithExactPartition

fun main() {
    val configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ConsumerConfig.GROUP_ID_CONFIG] = GROUP_ID
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

    val consumer = KafkaConsumer<String, String>(configs)
    consumer.assign(Collections.singleton(TopicPartition(TOPIC_NAME, TOPIC_NUMBER)))

    while (true) {
        val records = consumer.poll(Duration.ofSeconds(1))
        for (record in records) {
            logger.info("record:{}", record)
        }
    }
}