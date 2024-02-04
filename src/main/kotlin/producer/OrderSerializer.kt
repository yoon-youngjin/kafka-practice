package producer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import model.OrderModel
import org.apache.kafka.common.serialization.Serializer

class OrderSerializer : Serializer<OrderModel> {
    private val objectMapper = ObjectMapper().registerModules(JavaTimeModule())
    override fun serialize(topic: String?, data: OrderModel?): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}