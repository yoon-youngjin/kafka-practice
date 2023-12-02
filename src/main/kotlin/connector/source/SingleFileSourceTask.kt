package connector.source

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Collections
import java.util.stream.Collectors
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.slf4j.LoggerFactory

private var logger = LoggerFactory.getLogger(SingleFileSourceConnector::class.java)

class SingleFileSourceTask : SourceTask() {
    val FILENAME_FIELD = "filename"
    val POSITION_FIELD = "position"
    private var fileNamePartition: Map<String, String?>? = null
    private var offset: Map<String, Any>? = null
    private var topic: String? = null
    private var file: String? = null
    private var position: Long = -1
    override fun version(): String {
        return "1.0"
    }

    // 리소스를 초기화하는 용도 즉, 파일에 대한 정보를 초기화한다.
    override fun start(props: Map<String, String>) {
        try {
            // Init variables
            val config = SingleFileSourceConnectorConfig(props)
            topic = config.getString(SingleFileSourceConnectorConfig.TOPIC_NAME)
            file = config.getString(SingleFileSourceConnectorConfig.DIR_FILE_NAME)
            fileNamePartition = Collections.singletonMap(FILENAME_FIELD, file)
            offset = context.offsetStorageReader().offset(fileNamePartition)
            // 소스 태스크 내부에서 관리하는 오프셋 -> 해당 태스크에서는 내부 줄 번호를 기록하는 용도

            // Get file offset from offsetStorageReader
            if (offset != null) {
                val lastReadFileOffset = offset!![POSITION_FIELD]
                if (lastReadFileOffset != null) {
                    position = lastReadFileOffset as Long
                }
            } else { // 최초로 데이터를 가져온 경우
                position = 0
            }
        } catch (e: Exception) {
            throw ConnectException(e.message, e)
        }
    }

    // 주기적으로 실행되어서 데이터를 처리하는 로직 구현
    // 해당 메서드에서 리턴한 변수(List<SourceRecord>)는 커넥트의 send()에 의해서 토픽에 적재된다.
    override fun poll(): List<SourceRecord> {
        val results: MutableList<SourceRecord> = ArrayList()
        return try {
            Thread.sleep(1000)
            val lines = getLines(position) // 가져가고 싶은 position(내부 줄 번호)부터 데이터를 읽어감.
            if (lines.isNotEmpty()) {
                lines.forEach { line: String? ->
                    val sourceOffset =
                        Collections.singletonMap(POSITION_FIELD, ++position)
                    val sourceRecord =
                        SourceRecord(fileNamePartition, sourceOffset, topic, Schema.STRING_SCHEMA, line)
                    results.add(sourceRecord) // 토픽으로 보내고 싶은 데이터는 List<SourceRecord>에 element 추가
                }
            }
            results // 최종적으로 전달되는 List
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw ConnectException(e.message, e)
        }
    }

    @Throws(Exception::class)
    private fun getLines(readLine: Long): List<String> {
        val reader = Files.newBufferedReader(Paths.get(file))
        return reader.lines().skip(readLine).collect(Collectors.toList())
    }

    override fun stop() {}
}