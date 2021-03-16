
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.time.ExperimentalTime

class AWSSecretHelper: SecretHelperInterface {

    companion object {
        val logger = DataworksLogger.getLogger(AWSSecretHelper::class)
    }

    @ExperimentalTime
    override fun getSecret(secretName: String): String? {

        logger.info("Getting value from aws secret manager", "secret_name" to secretName)

        try {
            val region: String = Config.SecretManager.properties.getProperty("region")
            val client = AWSSecretsManagerClientBuilder.standard().withRegion(region).build()
            val getSecretValueRequest = GetSecretValueRequest().withSecretId(secretName)

            val getSecretValueResult = client.getSecretValue(getSecretValueRequest)

            logger.debug("Successfully got value from aws secret manager", "secret_name" to secretName)

            val secretString = getSecretValueResult.secretString

            @Suppress("UNCHECKED_CAST")
            val map = ObjectMapper().readValue(secretString, Map::class.java) as Map<String, String>

            return map["password"]
        } catch (e: Exception) {
            logger.error("Failed to get aws secret manager result", e, "secret_name" to secretName)
            throw e
        }
    }
}
