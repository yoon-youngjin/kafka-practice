package streams

import java.util.Properties
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig

private const val APPLICATION_NAME = "global-table-join-application"
private const val BOOTSTRAP_SERVERS = "localhost:9092"
private const val ADDRESS_GLOBAL_TABLE = "address_v2"
private const val ORDER_STREAM = "order"
private const val ORDER_JOIN_STREAM = "order_join"

class KStreamJoinGlobalKTable

fun main() {
    val props = Properties()
    props[StreamsConfig.APPLICATION_ID_CONFIG] = APPLICATION_NAME
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
    props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass

    val builder = StreamsBuilder()
    val addressGlobalTable = builder.globalTable<String, String>(ADDRESS_GLOBAL_TABLE) // 소스 프로세스
    val orderStream = builder.stream<String, String>(ORDER_STREAM) // 소스 프로세스

    orderStream.join(
        addressGlobalTable,
        { orderKey, _ -> orderKey },
        { order, address -> "$order send to $address" })
        .to(ORDER_JOIN_STREAM)

    val streams = KafkaStreams(builder.build(), props)
    streams.start()
}
