package connector

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

class TestSinkConnector: SinkConnector() {
    override fun version(): String {
        TODO("Not yet implemented")
    }

    override fun start(props: MutableMap<String, String>?) {
        TODO("Not yet implemented")
    }

    override fun taskClass(): Class<out Task> {
        TODO("Not yet implemented")
    }

    override fun taskConfigs(maxTasks: Int): MutableList<MutableMap<String, String>> {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun config(): ConfigDef {
        TODO("Not yet implemented")
    }
}