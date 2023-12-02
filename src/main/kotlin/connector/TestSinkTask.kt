package connector

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.SinkRecord
import org.apache.kafka.connect.sink.SinkTask

class TestSinkTask: SinkTask() {
    override fun version(): String {
        TODO("Not yet implemented")
    }

    override fun start(props: MutableMap<String, String>?) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun put(records: MutableCollection<SinkRecord>?) {
        TODO("Not yet implemented")
    }

    override fun flush(currentOffsets: MutableMap<TopicPartition, OffsetAndMetadata>?) {
        super.flush(currentOffsets)
    }
}