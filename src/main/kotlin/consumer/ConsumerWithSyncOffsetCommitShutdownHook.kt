package consumer

import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ConsumerWithSyncOffsetCommitShutdownHook::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
private const val GROUP_ID = "test-group"
private lateinit var configs: Properties

private lateinit var consumer : KafkaConsumer<String, String>

class ShutdownThread : Thread() {
    override fun run() {
        logger.info("Shutdown hook")
        consumer.wakeup()
    }
}

class ConsumerWithSyncOffsetCommitShutdownHook

fun main() {
    Runtime.getRuntime().addShutdownHook(ShutdownThread()) // 이렇게 추가하면 실행중인 애플리케이션에 shutdown 훅을 날릴 수 있고, 이때 wakeup 메서드를 통해서 안전하게 컨슈머 애플리케이션을 종료할 수 있다.
    configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ConsumerConfig.GROUP_ID_CONFIG] = GROUP_ID
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

    consumer = KafkaConsumer<String, String>(configs)
    consumer.subscribe(listOf(TOPIC_NAME))

    try {
        while (true) {
            val records = consumer.poll(Duration.ofSeconds(1))
            for (record in records) {
                logger.info("record:{}", record)
            }
        }
        consumer.commitSync()
    } catch (e: WakeupException) {
        logger.warn("Wakeup consumer")
    } finally {
        logger.warn("Consumer close");
        consumer.close();
    }
}