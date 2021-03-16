import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TextUtils {

    private val qualifiedTablePattern = Regex(Config.Hbase.qualifiedTablePattern)
    private val coalescedNames = mapOf("agent-core:agentToDoArchive" to "agent-core:agentToDo")

    fun topicNameTableMatcher(topicName: String) = qualifiedTablePattern.find(topicName)

    fun qualifiedTableName(topic: String): String? {
        val matcher = topicNameTableMatcher(topic)
        return if (matcher != null) {
            val namespace = matcher.groupValues[1]
            val tableName = matcher.groupValues[2]
            targetTable(namespace, tableName)
        }
        else {
            logger.error("Could not derive table name", "topic" to topic)
            null
        }
    }

    fun targetTable(namespace: String, tableName: String) =
        coalescedName("$namespace:$tableName").replace("-", "_").replace(".", "_")

    fun coalescedName(tableName: String): String =
        if (coalescedNames[tableName] != null) coalescedNames[tableName]!! else tableName

    fun printableKey(key: ByteArray) =
            if (key.size > 4) {
                val hash = key.slice(IntRange(0, 3))
                val hex = hash.joinToString("") { String.format("\\x%02X", it) }
                val renderable = key.slice(IntRange(4, key.size - 1)).map { it.toChar() }.joinToString("")
                "${hex}${renderable}"
            }
            else {
                String(key)
            }

    companion object {
        private val logger = DataworksLogger.getLogger(TextUtils::class)
    }

}
