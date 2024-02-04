package consumer

import io.confluent.parallelconsumer.ParallelConsumerOptions
import io.confluent.parallelconsumer.ParallelConsumerOptions.ProcessingOrder.UNORDERED
import io.confluent.parallelconsumer.ParallelStreamProcessor
import io.confluent.parallelconsumer.PollContext
import java.time.LocalDateTime
import java.util.Properties
import model.OrderModel
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ParallelConsumer::class.java)

class ParallelConsumer

fun main() {
    // Kafka Consumer 설정
    val configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
    configs[ConsumerConfig.GROUP_ID_CONFIG] = "parallel-test-group"
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = OrderDeserializer::class.java.name
    configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
//    configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

    // Kafka Consumer 인스턴스 생성
    val consumer = KafkaConsumer<String, OrderModel>(configs)

    // Parallel Consumer 설정
    val options = ParallelConsumerOptions.builder<String, OrderModel>()
        .ordering(UNORDERED)
        .maxConcurrency(100) // <3> -> dafualt 16
        .consumer(consumer)
        .build()

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

    // Parallel Stream Processor 생성 및 구독 시작
    val parallelConsumer = ParallelStreamProcessor.createEosStreamProcessor(options)
    parallelConsumer.subscribe(listOf("pizza-topic"))

    parallelConsumer.poll { context -> // 쓰레드 하나당 처리할 작업
        processRecord(context, orderDBHandler)
    }
}

// 실제 메시지 처리 로직
fun processRecord(context: PollContext<String, OrderModel>, orderDBHandler: OrderDBHandler) {
    for (record in context.consumerRecordsFlattened) {
        Thread.sleep(10)
        logger.info("${LocalDateTime.now()}, ${record.offset()}")
        orderDBHandler.insertOrder(record.value(), LocalDateTime.now())
    }
}
