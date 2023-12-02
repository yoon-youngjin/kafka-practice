package connector.source

import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigDef

class SingleFileSourceConnectorConfig(props: Map<String, String>?) : AbstractConfig(CONFIG, props) {
    companion object {
        const val DIR_FILE_NAME = "file" // key 값
        private const val DIR_FILE_NAME_DEFAULT_VALUE = "/tmp/kafka.txt"
        private const val DIR_FILE_NAME_DOC = "읽을 파일 경로와 이름"

        const val TOPIC_NAME = "topic" // key 값
        private const val TOPIC_DEFAULT_VALUE = "test"
        private const val TOPIC_DOC = "보낼 토픽명"

        val CONFIG: ConfigDef = ConfigDef().define(
            DIR_FILE_NAME,
            ConfigDef.Type.STRING,
            DIR_FILE_NAME_DEFAULT_VALUE,
            ConfigDef.Importance.HIGH,
            DIR_FILE_NAME_DOC
        )
            .define(
                TOPIC_NAME,
                ConfigDef.Type.STRING,
                TOPIC_DEFAULT_VALUE,
                ConfigDef.Importance.HIGH,
                TOPIC_DOC
            )
    }
}