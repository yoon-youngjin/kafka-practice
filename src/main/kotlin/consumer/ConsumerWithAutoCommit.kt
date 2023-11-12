package consumer

import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ConsumerWithAutoCommit::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
private const val GROUP_ID = "test-group"


class ConsumerWithAutoCommit

fun main() {
    val configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ConsumerConfig.GROUP_ID_CONFIG] = GROUP_ID
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

    configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = true // 기본값이 true라서 사실 명시할 필요 없다.
    configs[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = 60000  // poll()이 실행될 때 지정한 ms마다 커밋이 발생한다.

    val consumer = KafkaConsumer<String, String>(configs)
    consumer.subscribe(listOf(TOPIC_NAME))

    while (true) {
        val records = consumer.poll(Duration.ofSeconds(1))
        for (record in records) {
            logger.info("record:{}", record)
        }
    }
}