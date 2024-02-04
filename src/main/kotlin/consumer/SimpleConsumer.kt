package consumer

import java.time.Duration
import java.time.LocalDateTime
import java.util.Properties
import model.OrderModel
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
private const val GROUP_ID = "simple-test-group-03"

private val logger = LoggerFactory.getLogger(SimpleConsumer::class.java)

class SimpleConsumer

fun main() {
    val configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ConsumerConfig.GROUP_ID_CONFIG] = GROUP_ID
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = OrderDeserializer::class.java.name
//    configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

    val consumer = KafkaConsumer<String, OrderModel>(configs)
    consumer.subscribe(listOf("pizza-topic"))

    val url = "jdbc:mysql://localhost:3306/mysql"
    val user = "root"
    val password = ""
    val orderDBHandler = OrderDBHandler(url, user, password)

    val mainThread = Thread.currentThread() // main thread
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            consumer.wakeup()
            mainThread.join()
        }
    })

    while (true) {
        val records = consumer.poll(Duration.ofSeconds(100))
        processRecord(records, orderDBHandler)
    }
}

private fun processRecord(records: ConsumerRecords<String, OrderModel>, orderDBHandler: OrderDBHandler) {
    for (record in records) {
        Thread.sleep(10)
        logger.info("${LocalDateTime.now()}, ${record.offset()}")
        orderDBHandler.insertOrder(record.value(), LocalDateTime.now())
    }
}