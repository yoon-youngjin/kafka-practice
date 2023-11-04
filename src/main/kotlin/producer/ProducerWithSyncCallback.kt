package producer

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ProducerWithSyncCallback::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"

class ProducerWithSyncCallback

fun main() {
    val configs = Properties()
    configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

    val producer = KafkaProducer<String, String>(configs)

    val record = ProducerRecord(TOPIC_NAME, "Pangyo", "Pangyo")

    try {
        val metadata = producer.send(record).get()
        println("================")
        logger.info(metadata.toString())
    } catch (e: Exception) {
        logger.error(e.message, e)
    } finally {
        producer.flush()
        producer.close()
    }
}