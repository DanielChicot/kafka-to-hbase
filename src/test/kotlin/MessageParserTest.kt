import com.beust.klaxon.JsonObject
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MessageParserTest : StringSpec({

    val convertor = Converter()

    fun validateKeys(jsonOne: JsonObject, jsonTwo: JsonObject, contentShouldBeEqual: Boolean) {
        val parser = MessageParser()

        val (idOne, keyOne) = parser.generateKey(jsonOne)
        val (idTwo, keyTwo) = parser.generateKey(jsonTwo)

        keyOne.contentEquals(keyTwo) shouldBe contentShouldBeEqual
        idOne.contentEquals(idTwo) shouldBe contentShouldBeEqual
    }

    "generated keys are consistent for identical inputs" {
        val json: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        
        validateKeys(json, json, true)
    }

    "generated keys are different for different inputs" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":3}".toByteArray())

        validateKeys(jsonOne, jsonTwo, false)
    }

    "generated keys are consistent for identical inputs regardless of order" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testTwo\":2, \"testOne\":\"test1\"}".toByteArray())

        validateKeys(jsonOne, jsonTwo, true)
    }

    "generated keys are consistent for identical inputs regardless of whitespace" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{    \"testOne\":              \"test1\",            \"testTwo\":  2}".toByteArray())

        validateKeys(jsonOne, jsonTwo, true)
    }

    "generated keys are consistent for identical inputs regardless of order and whitespace" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{    \"testTwo\":              2,            \"testOne\":  \"test1\"}".toByteArray())

        validateKeys(jsonOne, jsonTwo, true)
    }

    "generated keys will vary given values with different whitespace" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testOne\":\"test 1\", \"testTwo\":2}".toByteArray())

        validateKeys(jsonOne, jsonTwo, false)
    }

    "generated keys will vary given values that are string and int in each input" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":\"2\"}".toByteArray())

        validateKeys(jsonOne, jsonTwo, false)
    }

    "generated keys will vary given values that are string and float in each input" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":2.0}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":\"2.0\"}".toByteArray())

        validateKeys(jsonOne, jsonTwo, false)
    }

    "generated keys will vary given values that are string and boolean in each input" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":false}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":\"false\"}".toByteArray())

        validateKeys(jsonOne, jsonTwo, false)
    }

    "generated keys will vary given values that are string and null in each input" {
        val jsonOne: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":null}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"testOne\":\"test1\", \"testTwo\":\"null\"}".toByteArray())

        validateKeys(jsonOne, jsonTwo, false)
    }

    "id is returned from valid json" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson(
            """{"message":{"_id":{"test_key":"test_value"}}}""".toByteArray())
        val idString = """{"test_key":"test_value"}"""

        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson?.toJsonString() shouldBe idString
    }

    "string id is returned from valid json" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson(
            """{"message":{"_id":"value"}}""".toByteArray())
        val idString = """{"id":"value"}"""

        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson?.toJsonString() shouldBe idString
    }

    "integer id is returned from valid json" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson(
            """{"message":{"_id":123}}""".toByteArray())
        val idString = """{"id":"123"}"""

        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson?.toJsonString() shouldBe idString
    }

    "null is returned from json where message does not exist" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("{\"test_object\":{\"_id\":{\"test_key\":\"test_value\"}}}".toByteArray())
        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson shouldBe null
    }

    "null is returned from json where message is not an object" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("{\"message\":\"test_value\"}".toByteArray())
        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson shouldBe null
    }

    "null is returned from json where _id is missing" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("{\"message\":{\"test_object\":{\"test_key\":\"test_value\"}}}".toByteArray())
        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson shouldBe null
    }

    "null is returned from json where _id is an array" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("""{"message":{"_id":["test_value"]}}""".toByteArray())
        val idJson: JsonObject? = parser.getId(jsonOne)

        idJson shouldBe null
    }

    "generated key is consistent for identical record body independant of key order and whitespace" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("{\"message\":{\"_id\":{\"test_key_a\":\"test_value_a\",\"test_key_b\"    :\"test_value_b\"}}}".toByteArray())
        val jsonTwo: JsonObject = convertor.convertToJson("{\"message\":{\"_id\":{\"test_key_b\":     \"test_value_b\",\"test_key_a\":\"test_value_a\"}}}".toByteArray())

        val (unformattedIdOne, keyOne) = parser.generateKeyFromRecordBody(jsonOne)
        val (unformattedIdTwo, keyTwo) = parser.generateKeyFromRecordBody(jsonTwo)

        keyOne shouldNotBe ByteArray(0)
        keyOne shouldBe keyTwo

        unformattedIdOne shouldBe "{\"test_key_a\":\"test_value_a\",\"test_key_b\":\"test_value_b\"}"
        unformattedIdTwo shouldBe "{\"test_key_a\":\"test_value_a\",\"test_key_b\":\"test_value_b\"}"
    }

    "empty is returned from record body key generation where message does not exist" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("{\"test_object\":{\"_id\":{\"test_key\":\"test_value\"}}}".toByteArray())
        val (unformattedId, key) = parser.generateKeyFromRecordBody(jsonOne)

        key shouldBe ByteArray(0)
        unformattedId shouldBe null
    }

    "empty is returned from record body key generation where message is not an object" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("{\"message\":\"test_value\"}".toByteArray())
        val (unformattedId, key) = parser.generateKeyFromRecordBody(jsonOne)

        key shouldBe ByteArray(0)
        unformattedId shouldBe null
    }

    "empty is returned from record body key generation where _id is missing" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("""{"message":{"test_object":{"test_key":"test_value"}}}""".toByteArray())
        val (unformattedId, key) = parser.generateKeyFromRecordBody(jsonOne)

        key shouldBe ByteArray(0)
        unformattedId shouldBe null
    }

    "empty is returned from record body key generation where _id is an array" {
        val parser = MessageParser()
        val jsonOne: JsonObject = convertor.convertToJson("""{"message":{"_id":["test_value"]}}""".toByteArray())
        val (unformattedId, key) = parser.generateKeyFromRecordBody(jsonOne)

        key shouldBe ByteArray(0)
        unformattedId shouldBe null
    }
})
