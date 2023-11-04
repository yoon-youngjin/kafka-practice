package producer

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(SimpleProducer::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"

class SimpleProducer

fun main() {
    val configs = Properties()
    configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

    val producer = KafkaProducer<String, String>(configs)

    val messageValue = "testMessage"
    val record = ProducerRecord<String, String>(TOPIC_NAME, messageValue)
    producer.send(record)
    logger.info("{}", record)
    producer.flush()
    producer.close()
}

