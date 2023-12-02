package connector.sink

import connector.source.SingleFileSourceConnectorConfig.Companion.DIR_FILE_NAME
import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigDef

// 어떠한 토픽으로부터 데이터를 받는지 여기서 설정할 필요가 없다.
// 싱크 커넥터를 실행할 때 어떤 토픽으로부터 데이터를 받는지 지정해야한다.
class SingleFileSinkConnectorConfig(props: Map<String, String>?) : AbstractConfig(CONFIG, props) {
    val DIR_FILE_NAME = "file"
    companion object {
        // 해당 파일에 데이터를 저장한다. -> 해당 값을 지정하지 않으면 DIR_FILE_NAME_DEFAULT_VALUE에 저장된다.
        private const val DIR_FILE_NAME_DEFAULT_VALUE = "/tmp/kafka.txt"
        private const val DIR_FILE_NAME_DOC = "저장할 디렉토리와 파일 이름"

        val CONFIG: ConfigDef = ConfigDef().define(
            DIR_FILE_NAME,
            ConfigDef.Type.STRING,
            DIR_FILE_NAME_DEFAULT_VALUE,
            ConfigDef.Importance.HIGH,
            DIR_FILE_NAME_DOC
        )
    }
}
