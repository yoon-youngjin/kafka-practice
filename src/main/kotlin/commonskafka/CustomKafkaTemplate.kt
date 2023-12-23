package commonskafka

import org.springframework.kafka.core.KafkaTemplate

class CustomKafkaTemplate<K, V>(
    private val customKafkaTemplate: KafkaTemplate<K?, V>,
    private val kafkaDLTemplate: KafkaTemplate<K?, V>,
    private val service: String? = null,
    private val dlTopic: String? = "PDL-PRE",
) {

}