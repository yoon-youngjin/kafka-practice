package producer.file

data class MessageEvent<K, V>(
    val key: K? = null,
    val value: V,
)