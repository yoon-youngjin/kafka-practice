package producer

import java.io.BufferedReader
import java.io.FileReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import model.OrderModel
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

private const val TOPIC_NAME = "pizza-topic"
private const val BOOTSTRAP_SERVERS = "localhost:9092"
private val logger = LoggerFactory.getLogger(FileProducer::class.java)

class FileProducer

fun main() {
    val props = Properties()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = OrderSerializer::class.java.name

    val producer = KafkaProducer<String, OrderModel>(props)
    val filePath = "/Users/dudwls143/Workspace/KafkaProj-01/practice/src/main/resources/pizza_sample.txt"
    sendFileMessages(producer, filePath)

    producer.close() // 종료
}

fun sendFileMessages(producer: KafkaProducer<String, OrderModel>, filePath: String) {
    val reader = BufferedReader(FileReader(filePath))
    val delimiter = ","
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    reader.forEachLine { line ->
        val tokens = line.split(delimiter)
        val key = tokens.first()
        val value = OrderModel(
            orderId = tokens[1],
            shopId = tokens[2],
            menuName = tokens[3],
            userName = tokens[4],
            phoneNumber = tokens[5],
            address = tokens[6],
            orderTime = LocalDateTime.parse(tokens[7].trim(), formatter)
        )

        sendMessage(producer, key, value)
    }
}

private fun sendMessage(producer: KafkaProducer<String, OrderModel>, key: String, value: OrderModel) {
    val record = ProducerRecord(TOPIC_NAME, key, value)
    logger.info("key:$key, value:$value")
    for (i in (1..50)) {
        producer.send(record) { metadata, exception ->
            if (exception == null && metadata != null) {
                logger.info(
                    "\n ##### record metadata received #### \n" +
                        "partition: ${metadata.partition()}\n" +
                        "offset: ${metadata.offset()}\n" +
                        "timestamp: ${metadata.timestamp()}\n"
                )
            } else {
                logger.error("exception error from broker ${exception.message}")
            }
        }
    }
}
