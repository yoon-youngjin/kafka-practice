package producer

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.Cluster
import org.apache.kafka.common.InvalidRecordException
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.utils.Utils
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ProducerWithCustomPartitioner::class.java)

private const val TOPIC_NAME = "test"
private const val BOOTSTRAP_SERVERS = "127.0.0.1:9092"
class ProducerWithCustomPartitioner

class CustomPartitioner : Partitioner {
    override fun configure(configs: MutableMap<String, *>?) {}

    override fun close() {}

    override fun partition(
        topic: String?,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster?,
    ): Int {
        if (keyBytes == null) throw InvalidRecordException("Need message key")

        if (key == "Pangyo") return 0

        val partitions = cluster!!.partitionsForTopic(topic)
        val numPartitions = partitions.size
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions
    }
}

fun main() {
    val configs = Properties()
    configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.PARTITIONER_CLASS_CONFIG] = CustomPartitioner::class.java

    val producer = KafkaProducer<String, String>(configs)

    val partitionNo = 0
    val record = ProducerRecord(TOPIC_NAME, partitionNo, "Pangyo", "Pangyo")
    producer.send(record)


    logger.info("{}", record)

    producer.flush()
    producer.close()
}