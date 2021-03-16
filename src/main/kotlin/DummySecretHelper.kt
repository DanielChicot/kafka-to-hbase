class DummySecretHelper: SecretHelperInterface {
    override fun getSecret(secretName: String): String = "password"
}
