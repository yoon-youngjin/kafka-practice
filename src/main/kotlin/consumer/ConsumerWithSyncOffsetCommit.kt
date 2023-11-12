package consumer

import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger(ConsumerWithSyncOffsetCommit::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
private const val GROUP_ID = "test-group"
private lateinit var configs: Properties

private lateinit var consumer: KafkaConsumer<String, String>


class ConsumerWithSyncOffsetCommit

fun main() {
    configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ConsumerConfig.GROUP_ID_CONFIG] = GROUP_ID
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false

    consumer = KafkaConsumer<String, String>(configs)
    consumer.subscribe(listOf(TOPIC_NAME))

    while (true) {
        val records = consumer.poll(Duration.ofSeconds(1))
        val currentOffset = HashMap<TopicPartition, OffsetAndMetadata>()

        for (record in records) {
            logger.info("record:{}", record)
            currentOffset[TopicPartition(record.topic(), record.partition())] =
                OffsetAndMetadata(record.offset() + 1, null)
            consumer.commitSync(currentOffset)
        }
    }
}