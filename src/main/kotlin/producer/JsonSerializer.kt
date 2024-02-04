package producer

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer


class JsonSerializer<T>(
    private val objectMapper: ObjectMapper,
) : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray? {
        return data?.let {
            try {
                objectMapper.writeValueAsBytes(it)
            } catch (e: IOException) {
                throw SerializationException(
                    "Can't serialize data [$it] for topic [$topic]",
                    e
                )
            }
        }
    }
}