package producer

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ProducerWithKeyValue::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
class ProducerWithKeyValue

fun main() {
    val configs = Properties()
    configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    //configs.put(ProducerConfig.ACKS_CONFIG, "0");

    val producer = KafkaProducer<String, String>(configs)

    val record1 = ProducerRecord(TOPIC_NAME, "Pangyo", "Pangyo")
    producer.send(record1)
    val record2 = ProducerRecord(TOPIC_NAME, "Busan", "Busan")
    producer.send(record2)

    logger.info("{}", record1)
    logger.info("{}", record2)

    producer.flush()
    producer.close()
}