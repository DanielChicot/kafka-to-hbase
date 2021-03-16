import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
object TestUtils {
    fun defaultMessageValidator() {
        Config.Validator.properties = Properties().apply {
            put(Config.schemaFileProperty, Config.mainSchemaFile)
        }
    }

    fun equalityMessageValidator() {
        Config.Validator.properties = Properties().apply {
            put(Config.schemaFileProperty, Config.equalitySchemaFile)
        }
    }

    fun auditMessageValidator() {
        Config.Validator.properties = Properties().apply {
            put(Config.schemaFileProperty, Config.auditSchemaFile)
        }
    }
}
