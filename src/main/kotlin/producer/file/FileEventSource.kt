package producer.file

import com.practice.kafka.event.EventHandler
import java.io.File
import java.io.RandomAccessFile
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FileEventSource::class.java)

class FileEventSource(
    private val updateInterval: Long,
    private val file: File,
    private val eventHandler: EventHandler<String?, String>,
) : Runnable {

    private val keepRunning: Boolean = true
    private var filePointer: Long = 0L // 이전에 읽은 파일의 length

    override fun run() {
        while (this.keepRunning) {
            Thread.sleep(updateInterval)
            val len = file.length()

            if (len < filePointer) {
                logger.info("file was reset as filePointer is logger than file length!")
                filePointer = len
            } else if (len > filePointer) { // 파일 변화
                readAppendAndSend()
            }
        }
    }

    private fun readAppendAndSend() {
        RandomAccessFile(file, "r").use { file ->
            file.seek(filePointer) // 0부터 읽는다.
            while (true) {
                val line = file.readLine() ?: break
                sendMessage(line)
            }
            filePointer = file.length()
        }
    }

    private fun sendMessage(line: String) {
        val delimiter = ","

        val tokens = line.split(delimiter)
        val key = tokens.firstOrNull()
        val value = tokens.drop(1).joinToString(delimiter)

        val messageEvent = MessageEvent<String?, String>(
            key = key,
            value = value,
        )
        eventHandler.onMessage(messageEvent)
    }
}