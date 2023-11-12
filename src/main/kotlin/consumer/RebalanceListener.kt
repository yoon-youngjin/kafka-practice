package consumer

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(RebalanceListener::class.java)

class RebalanceListener : ConsumerRebalanceListener {
    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        logger.warn("Partitions are assigned : " + partitions.toString())
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        logger.warn("Partitions are revoked : " + partitions.toString())
    }
}