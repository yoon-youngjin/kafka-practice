package consumer

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import model.OrderModel
import org.apache.kafka.common.serialization.Deserializer

class OrderDeserializer : Deserializer<OrderModel> {
    private val objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule()) // jacksonObjectMapper 에 코틀린 모듈이 포함
    override fun deserialize(topic: String?, data: ByteArray?): OrderModel {
        return objectMapper.readValue(data, OrderModel::class.java)
    }
}