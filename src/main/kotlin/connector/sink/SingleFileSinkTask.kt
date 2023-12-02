package connector.sink

import java.io.File
import java.io.FileWriter
import java.io.IOException
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.sink.SinkRecord
import org.apache.kafka.connect.sink.SinkTask

// 파일에 데이터를 쓰고 싶기 때문에 파일 열기 쓰기 닫기를 해야한다.
class SingleFileSinkTask : SinkTask() {
    private lateinit var config: SingleFileSinkConnectorConfig
    private lateinit var file: File
    private lateinit var fileWriter: FileWriter
    override fun version() = "1.0"

    override fun start(props: Map<String, String>) {
        try {
            // Init variables
            config = SingleFileSinkConnectorConfig(props)
            file = File(config.getString(config.DIR_FILE_NAME))
            fileWriter = FileWriter(file, true)
        } catch (e: Exception) {
            throw ConnectException(e.message, e)
        }
    }

    override fun put(records: MutableCollection<SinkRecord>) {
        try {
            for (record in records) {
                fileWriter.write(record.value().toString() + "\n")
            }
        } catch (e: IOException) {
            throw ConnectException(e.message, e)
        }
    }

    override fun flush(currentOffsets: MutableMap<TopicPartition, OffsetAndMetadata>?) {
        try {
            fileWriter.flush()
        } catch (e: IOException) {
            throw ConnectException(e.message, e)
        }
    }

    override fun stop() {
        try {
            fileWriter.close()
        } catch (e: IOException) {
            throw ConnectException(e.message, e)
        }
    }
}
