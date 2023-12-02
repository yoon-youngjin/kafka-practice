package connector.source

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.source.SourceConnector

class SingleFileSourceConnector : SourceConnector() {

    private var configProperties: Map<String, String>? = null
    override fun version(): String {
        return "1.0"
    }

    override fun start(props: Map<String, String>?) {
        configProperties = props
        // props -> file, topic에 대해서 전달받는다.
        try {
            SingleFileSourceConnectorConfig(props)
        } catch (e: ConfigException) {
            throw ConnectException(e.message, e)
        }
    }

    override fun taskClass(): Class<out Task>? {
        return SingleFileSourceTask::class.java
    }

    // 태스크에 어떤 설정을 넣는지 지정하는 메서드
    // 아래는 모든 동일한 설정이 들어간다.
    override fun taskConfigs(maxTasks: Int): List<Map<String, String>> {
        val taskConfigs: MutableList<Map<String, String>> = ArrayList()
        val taskProps: MutableMap<String, String> = HashMap()
        taskProps.putAll(configProperties!!)
        for (i in 0..<maxTasks) {
            taskConfigs.add(taskProps)
        }
        return taskConfigs
    }

    override fun config(): ConfigDef {
        return SingleFileSourceConnectorConfig.CONFIG
    }

    override fun stop() {}
}