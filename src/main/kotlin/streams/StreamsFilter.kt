package streams

import java.util.Properties
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig


private const val APPLICATION_NAME = "streams-filter-application"
private const val BOOTSTRAP_SERVERS = "localhost:9092"
private const val STREAM_LOG = "stream_log"
private const val STREAM_LOG_FILTER = "stream_log_filter"

class StreamsFilter

fun main() {
    val props = Properties()
    props[StreamsConfig.APPLICATION_ID_CONFIG] = APPLICATION_NAME
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
    props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass

    val builder = StreamsBuilder()
    val streamLog = builder.stream<String, String>(STREAM_LOG) // 1. 소스 프로세서로 STREAM_LOG 토픽에서 스트림 데이터를 가져온다.

    val filteredStream = streamLog.filter { _, value -> value.length > 5 } // 2. 스트림 프로세서로 데이터를 가공한다.
    filteredStream.to(STREAM_LOG_FILTER) // 3. 싱크 프로세서를 통해 다른 토픽(STREAM_LOG_FILTER)에 데이터를 저장한다.

    // streamLog.filter { _, value -> value.length > 5 }.to(STREAM_LOG_FILTER)

    val streams = KafkaStreams(builder.build(), props)
    streams.start()


}