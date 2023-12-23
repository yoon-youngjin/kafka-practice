package commonskafka.producer

import java.lang.Exception
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

// 프로듀서가 메시지를 브로커로 보내기 전에 메시지를 가로채서 처리할 수 있다.
class CustomProducerInterceptor : ProducerInterceptor<Any?, Any> {

    override fun onSend(record: ProducerRecord<Any?, Any>): ProducerRecord<Any?, Any> {
        // MDC 조작 가능
        return record
    }

    override fun configure(configs: MutableMap<String, *>?) {}

    override fun onAcknowledgement(metadata: RecordMetadata?, exception: Exception?) {}

    override fun close() {}
}