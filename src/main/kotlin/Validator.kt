import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.time.ExperimentalTime

@ExperimentalTime
open class Validator {

    @Throws(InvalidMessageException::class)
    open fun validate(json: String) {
        try {
            val jsonObject = JSONObject(json)
            val schema = schema()
            schema.validate(jsonObject)
        } catch (e: ValidationException) {
            throw InvalidMessageException("Message failed schema validation: '${e.message}'.", e)
        }
    }

    private fun schema() = schemaLoader.load().build()

    private val schemaLoader: SchemaLoader by lazy {
        SchemaLoader.builder()
            .schemaJson(schemaObject())
            .draftV7Support()
            .build()
    }

    private fun schemaObject() =
        javaClass.getResourceAsStream(schemaLocation()).use { inputStream ->
            JSONObject(JSONTokener(inputStream))
        }

    private fun schemaLocation() =
        Config.Validator.properties["schema.location"] as String
}
