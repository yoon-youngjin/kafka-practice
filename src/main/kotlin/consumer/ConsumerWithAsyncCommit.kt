package consumer

import java.lang.Exception
import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.OffsetCommitCallback
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import kotlin.math.log

private val logger = LoggerFactory.getLogger(ConsumerWithAsyncCommit::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
private const val GROUP_ID = "test-group"

class ConsumerWithAsyncCommit

fun main() {
    val configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ConsumerConfig.GROUP_ID_CONFIG] = GROUP_ID
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

    configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false

    val consumer = KafkaConsumer<String, String>(configs)
    consumer.subscribe(listOf(TOPIC_NAME))

    while (true) {
        val records = consumer.poll(Duration.ofSeconds(1))
        for (record in records) {
            logger.info("record:{}", record)
        }

        // 콜백을 지정함으로써 브로커로부터 커밋 응답은 받지 않지만 스레드가 백그라운드에서 결과를 받아서 처리한다.
        consumer.commitAsync(object : OffsetCommitCallback {
            override fun onComplete(offsets: MutableMap<TopicPartition, OffsetAndMetadata>?, e: Exception?) {
                if (e != null) {
                    logger.error("Commit failed for offsets {}", offsets, e)
                } else
                    logger.info("Commit succeeded")
            }

        })
    }
}