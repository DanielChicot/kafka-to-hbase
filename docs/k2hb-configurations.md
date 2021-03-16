# Configuration

There are a number of environment variables that can be used to configure
the system. Some of them are for configuring Kafka2Hbase itself, and some
are for configuring the built-in ACM PCA client to perform mutual auth.

## Kafka2Hbase Configuration

#### Hbase

By default Kafka2Hbase will connect to Zookeeper at `zookeeper:2181` use the parent uri `hbase`
and create tables in the `k2hb` namespace. The data will be stored in `cf:data`
with at least `1` version and at most `10` versions and a TTL of 10 days.

* **K2HB_HBASE_ZOOKEEPER_PARENT**
    The hbase parent uri, defaults to `/hbase` but should be set to `/hbase-unsecure` for AWS HBase
* **K2HB_HBASE_ZOOKEEPER_QUORUM**
    Comma separated list of Zookeeper servers
* **K2HB_HBASE_ZOOKEEPER_PORT**
    The listening port of the Zookeeper servers
* **K2HB_HBASE_DATA_TABLE**
    The name of the table to store message bodies in
* **K2HB_HBASE_DATA_FAMILY**
    The name of the column family to store message bodies in
* **K2HB_HBASE_TOPIC_TABLE**
    The name of the table to store topic message counts in
* **K2HB_HBASE_TOPIC_FAMILY**
    The name of the column family to store topic message counts in
* **K2HB_HBASE_TOPIC_QUALIFIER**
    The name of the column qualifier to store topic message counts in

#### Kafka

By default Kafka2Hbase will connect to Kafka at `kafka:9092` in the `k2hb`
consumer group. It will poll the `test-topic` topic with a poll timeout of
`10` days, and refresh the topics list every 10 seconds (`10000` ms).

* **K2HB_KAFKA_BOOTSTRAP_SERVERS**
    Comma separated list of Kafka servers and ports
* **K2HB_KAFKA_CONSUMER_GROUP**
    The name of the consumer group to join
* **K2HB_KAFKA_TOPIC_REGEX**
    A regex that will fetch a list of topics to listen to, e.g. `db.*`. Defaults to `test-topic.*`
* **K2HB_KAFKA_META_REFRESH_MS** (Optional)
    The frequency that the consumer will ask the broker for metadata updates, which also checks for new topics.
    Defaults to `10000` ms (10 seconds).
    Typically, should be an order of magnitude less than `K2HB_KAFKA_POLL_TIMEOUT`, else new topics will not be discovered within each polling interval.
* **K2HB_KAFKA_POLL_TIMEOUT**
    The maximum time to wait for messages in ISO-8601 duration format (e.g. `PT10S`).
    Defaults to 1 Hour.
    Should be greater than `K2HB_KAFKA_META_REFRESH_MS`, else new topics will not be discovered within each polling interval.
* **K2HB_KAFKA_INSECURE**
    Disable SSL entirely (useful for dev / test) with `K2HB_KAFKA_INSECURE=true`
* **K2HB_KAFKA_CERT_MODE**
    If SSL is enabled, either create certs in ACM-PCA with value `CERTGEN` or retrieve
    them from ACM with value `RETRIEVE`

#### SSL Mutual Authentication (CERTGEN mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full or disabled entirely via `K2HB_KAFKA_INSECURE=FALSE`
and `K2HB_KAFKA_CERT_MODE=CERTGEN`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script.

* **CERTGEN_CA_ARN**
    The AWS CA ARN to use to generate the cert
* **CERTGEN_KEY_TYPE**
    The type of private key (`RSA` or `DSA`)
* **CERTGEN_KEY_LENGTH**
    The key length in bits (`1024`, `2048` or `4096`)
* **CERTGEN_KEY_DIGEST**
    The key digest algorithm (`sha256`, `sha384`, `sha512`)
* **CERTGEN_SUBJECT_C**
    The subject country
* **CERTGEN_SUBJECT_ST**
    The subject state/province/county
* **CERTGEN_SUBJECT_L**
    The subject locality
* **CERTGEN_SUBJECT_O**
    The subject organisation
* **CERTGEN_SUBJECT_OU**
    The subject organisational unit
* **CERTGEN_SUBJECT_EMAILADDRESS**
    The subject email address
* **CERTGEN_SIGNING_ALGORITHM**
    The certificate signing algorithm used by ACM PCA
    (`SHA256WITHECDSA`, `SHA384WITHECDSA`, `SHA512WITHECDSA`, `SHA256WITHRSA`, `SHA384WITHRSA`, `SHA512WITHRSA`)
* **CERTGEN_VALIDITY_PERIOD**
    The certificate validity period in Go style duration (e.g. `1y2m6d`)
* **CERTGEN_PRIVATE_KEY_ALIAS**
    Alias for the private key
* **CERTGEN_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **CERTGEN_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **CERTGEN_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)


#### SSL Mutual Authentication (RETRIEVE mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full or disabled entirely via `K2HB_KAFKA_INSECURE=FALSE`
and `K2HB_KAFKA_CERT_MODE=RETRIEVE`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script.

* **RETRIEVER_ACM_CERT_ARN**
    ARN in AWS ACM to use to fetch the required cert, cert chain, and key
* **RETRIEVER_ADD_DOWNLOADED_CHAIN**
    Whether or not to add the downloaded cert chain from the ARN to the trust store
    Allowed missing, `true`, `false`, `yes`, `no`, `1` or `0`
    If missing defaults to false
* **RETRIEVE_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **RETRIEVE_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **RETRIEVE_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)

#### Metadatastore 

| Name | Notes |
|------|--------|
|K2HB_METADATA_STORE_TABLE| The name of the table to write metadata store entries to |
|K2HB_RDS_CA_CERT_PATH|Where the Amazon root CA is located|
|K2HB_RDS_DATABASE_NAME|The name of the database to connect to ('metadatastore for circle/local)|
|K2HB_RDS_ENDPOINT|The server to connect to ('metadatastore for circle/local)|
|K2HB_RDS_PASSWORD_SECRET_NAME|The name of the password secret to look up (should be 'password' for local/circle running)|
|K2HB_RDS_PORT|The port to connect on ('3306' for local and circle)|
|K2HB_RDS_USERNAME|The username to connect with ('k2hbwriter' for local usage and circle)|
|K2HB_USE_AWS_SECRETS|Whether to fetch metadatastore passwords from AWS (should be 'false' for local running and circle)|
|K2HB_USE_AWS_SECRETS|Whether to look up secrets in AWS ('false' for local/circle)|
|K2HB_WRITE_TO_METADATA_STORE| Whether to write to the metadata store - to enable us to feature toggle writes on and off |

#### AWS Service
| Name | Notes|
|------|------|
|K2HB_AWS_S3_ARCHIVE_BUCKET| The bucket to which the messages are written |
|K2HB_AWS_S3_ARCHIVE_DIRECTORY| The directory in the bucket under which messages are written (or the common part of the key/prefix) |
|K2HB_AWS_S3_BATCH_PUTS| Whether to write each batch of received messages for a topic/partition combination as one object rather than one object per message, should probably be 'true' for actual running in aws |
|K2HB_AWS_S3_MAX_CONNECTIONS| Default max concurrentl connections is 500, set this to deviate from that|
|K2HB_AWS_S3_PARALLEL_PUTS| If not batch putting allows all messages to be written in parallel - n.b. may cause rate limiting issues |
|K2HB_AWS_S3_REGION| Set this if the default is not where you want to point |
|K2HB_AWS_S3_USE_LOCALSTACK| Set to true for local running - uses containerized aws|
