import com.beust.klaxon.JsonObject

open class MessageParser {

    private val converter = Converter()

    open fun generateKeyFromRecordBody(body: JsonObject?): Pair<String?, ByteArray> {
        val id: JsonObject? = body?.let { getId(it) }
        return if (id == null) Pair(null, ByteArray(0)) else generateKey(id)
    }

    fun getId(json: JsonObject): JsonObject? {
        val message = json["message"]
        if (message != null && message is JsonObject) {
            val id = message["_id"]

            if (id != null) {
                when (id) {
                    is JsonObject -> {
                        return id
                    }
                    is String -> {
                        val idObject = JsonObject()
                        idObject["id"] = id
                        return idObject
                    }
                    is Int -> {
                        val idObject = JsonObject()
                        idObject["id"] = "$id"
                        return idObject
                    }
                    else -> {
                        return null
                    }
                }
            }
            else {
                return null
            }

        }
        else {
            return null
        }
    }

    fun generateKey(json: JsonObject): Pair<String, ByteArray> {
        val jsonOrdered = converter.sortJsonByKey(json)
        val checksumBytes: ByteArray = converter.generateFourByteChecksum(jsonOrdered)
        return Pair(jsonOrdered, checksumBytes.plus(jsonOrdered.toByteArray()))
    }
}
