package producer.file

import com.practice.kafka.event.EventHandler
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FileEventHandler::class.java)

class FileEventHandler(
    private val producer: KafkaProducer<String?, String>,
    private val topicName: String,
    private val sync: Boolean,
) : EventHandler<String?, String> {
    override fun onMessage(messageEvent: MessageEvent<String?, String>) {
        val record = ProducerRecord(topicName, messageEvent.key, messageEvent.value)
        if (this.sync) {
            val recordMetadata = producer.send(record).get()
            logger.info(
                "\n ##### record metadata received #### \n" +
                    "partition: ${recordMetadata.partition()}\n" +
                    "offset: ${recordMetadata.offset()}\n" +
                    "timestamp: ${recordMetadata.timestamp()}\n"
            )
        } else {
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
}