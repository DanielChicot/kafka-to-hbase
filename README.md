# Kafka to HBase

## Links

1. [Application Configurations](docs/k2hb-configurations.md)
1. [Local Development](docs/local-development.md)
1. [Kafka samples and tutorial](docs/kafka-tutorial-samples.md)
1. [Agreed Schemas with upstream sources](agreed_schemas_with_upstream_sources)

## Summary

Providing a way of migrating data in Kafka topics into tables in Hbase,
preserving versions based on Kafka message timestamps.

Two columns are written to for each message received; one to store the body
of the message and one to store a count and last received date of the
topic. These are configured using the `K2HB_KAFKA_TOPIC_*` and
`K2HB_KAFKA_DATA_*` environment variables.

By default, if the kafka topic is `db.database-name.collection-name` the data table is `database_name:collection_name` 
with a column family of `topic`. 
Collections support additional `.` characters but hbase does not, so from `db.database-name.coll-ection.name` we would 
get a table `database_name:coll_ection_name`

The qualifier is the topic name, the body of the cell is the raw message
received from Kafka and the version is the timestamp of the message in
milliseconds.

For example, after receiving a single message on `db.my.data` the data
is saved as follows:

```
hbase(main):001:0> scan 'my:data'
ROW                                       COLUMN+CELL
 63213667-c5a5-4411-a93b-e2da709c553e     column=topic:my:data, timestamp=1563547895682, value=<entire message body>
1 row(s) in 0.1090 seconds
```

Kafka2Hbase will attempt to create the required namespaces, tables and column families on startup - If they already exist, nothing will happen. 

By default, the data table column family has a maximum of MAXINT versions (approximately 2.1 billion) and a minimum of 1 version; There is no TTL.

## Agreed Schemas with upstream sources.

Currently, the only upstream source is the UC Kafka Broker.

All the schemas are found in [src/main/resources](src/main/resources)

### UC Common elements

* `$.@type`, `$.version`, and `$.timestamp` are standard wrapper elements, so they should always be present.
* All have a few root elements that are mandatory but may be null:
  * `$.unitOfWorkId` is nullable.  In practice it will always be there for the business messages, but UC don't enforce that and it wouldn't be wrong if it was null. UC use it for application-level transactions, so it's almost always present.  But there are circumstances where we do things outside of a transaction (usually for long-running batch jobs), so it's not guaranteed.
  * `$.traceId` can also be nullable, for the same reasons.  It would not be null in practice, but in terms of validation it wouldn't be invalid if it were null. UC log audit messages outside of a transaction sometimes, as those are generated on some read-only events.  It would be unusual to do so for DB-writes, but not impossible.  As for equality messages, that logic pathway is simple enough that they never write them outside of a transaction at present, but there's nothing stopping that changing in future, so it would be a better fit (in terms of business rules) if it was nullable there too.
* All require the `$.message` block.
* All require the `$.message.encryption` block, and carry the dbObject as encrypted data.
* We reject any message with a `$.message.dbObject` that looks like JSON.

### UC Business Data Schema

* For Business messages we should only assert that `$.message._id` exists, as what is in it can vary a lot per collection, in structure, type and content.
* Business messages always have `$.message.db`.
* Business messages always have `$.message.collection`.
* These are sourced from many topics, the name of which is deterministically related to the `db` and `collection`.
* Sample kafka message: [business-message-sample.json](docs/business-message-sample.json)
* Sample unencrypted message payload from `dbObject`: [business-message-sample-unencrypted-payload.json](docs/business-message-sample-unencrypted-payload.json)
  * Note that k2hb does not decrypt this, it is for reference only
  * Note that the payloads of each collection differ wildly.

### UC Equality Data Schema

* Equality messages will always have exactly `$.message._id.messageId="non-zero-string"` so we can insist on `messageId` always existing and being >1 in length.
* Equality messages do not have `$.message.db` or `$.message.collection` as this is non-specific Equality Act data like demographic spreads.
* These are sourced from a single static collection.
* Sample kafka message: [equality-message-sample.json](docs/equality-message-sample.json)
* Sample unencrypted message payload from `dbObject`: [equality-message-sample-unencrypted-payload.json](docs/equality-message-sample-unencrypted-payload.json)
  * Note that k2hb does not decrypt this, it is for reference only

### UC Audit Data Schema

* Audit messages will always have exactly `$.message._id.auditId="non-zero-string"` so we can insist on it always existing and being >1 in length.
* Audit messages do not have `$.message.db` or `$.message.collection` as this is non-specific Audit information like which user logged on to the system, or which type of update was performed.
* These are sourced from a single static collection.
* Sample kafka message: [audit-message-sample.json](docs/audit-message-sample.json)
* Sample unencrypted message payload from `dbObject`: [audit-message-sample-unencrypted-payload.json](docs/audit-message-sample-unencrypted-payload.json)
  * Note that k2hb does not decrypt this, it is for reference only

### Environment Variables
|Name|Purpose|
|----|---|                                                                                                                                                                                    
| K2HB_AWS_S3_BATCH_PUTS | A toggle for whether to use batch puts for writing to s3, e.g. `true` |                                                                                                                                                                          
| K2HB_AWS_S3_USE_LOCALSTACK | A toggle for whether to use localstack for local development, e.g. `true` |
| K2HB_HBASE_ZOOKEEPER_PARENT | The node in zookeeper that identifies hbase e.g. `/hbase` |                                                                                                                                                                  
| K2HB_HBASE_ZOOKEEPER_PORT | The port to use for connecting to zookeeper i.e. `2181` |                                                                                                                                                                    
| K2HB_HBASE_ZOOKEEPER_QUORUM | The zookeeper host e.g. `hbase.dataworks.dwp.gov.uk` |                                                                                                                                                                        
| K2HB_HBASE_OPERATION_TIMEOUT_MILLISECONDS | Top-level restriction for making sure a blocking operation in Table will not be blocked more than this |                                                                                                                                                                  
| K2HB_HBASE_PAUSE_MILLISECONDS | The time in milliseconds for hbase to pause when writing to Hbase, e.g. `50` |                                                                                                                                                                    
| K2HB_HBASE_RETRIES | The number of retries that will be attempted before failing to write to Hbase |                                                                                                                                                                  
| K2HB_HBASE_RPC_TIMEOUT_MILLISECONDS | How long HBase client applications take for a remote call to time out. |                                                                                                                                                          
| K2HB_KAFKA_DLQ_TOPIC | The topic to listen for on the kafka dlq, e.g. `test-dlq-topic` |                                                                                                                                                       
| K2HB_RDS_USER | The user to use when connecting to the metadatastore e.g. `k2hbwriter` |                                                                                                                                                                      
| K2HB_RDS_PASSWORD_SECRET_NAME | The name of the secret manager secret used to store the database users password  |                                                                                                                                                      
| K2HB_RDS_DATABASE_NAME | The database that holds the reconciler table e.g. `metadatastore` |                                                                                                                                                   
| K2HB_METADATA_STORE_TABLE | The reconciler database table we want to write to e.g. `ucfs` |                                                                                                                                                                        
| K2HB_RDS_ENDPOINT | The reconciler database host or endpoint |                                                                                                                                                                  
| K2HB_RDS_PORT | The port to connect to when establishing metadata store connection, e.g. `3306` |                                                                                                                                
| K2HB_RDS_CA_CERT_PATH | The certification location that is needed for authenticating with the rds database, e.g. `/certs/AmazonRootCA1.pem` |                                                                                                                                                                    
| K2HB_USE_AWS_SECRETS | Whether to look up the metadatastore password in aws secret manager |                                                                                                                                                           
| K2HB_KAFKA_INSECURE | A toggle for whether the connection for kafka is insecure |                                                                                                                                                
| K2HB_KAFKA_MAX_POLL_RECORDS | The number of unreconciled records to read at a time. |                                                                                                                                                      
| K2HB_KAFKA_META_REFRESH_MS | The refresh in milliseconds for kafka metadata, e.g. `1000` |                                                                                                                                                        
| K2HB_KAFKA_POLL_TIMEOUT | The timeout for how long it will wait when polling Kafka, e.g. `PT10S` |   
| K2HB_HBASE_REGION_SPLITS | The number of regions to set when k2hb creates a missing table e.g. `2` | 
| K2HB_WRITE_TO_METADATA_STORE | Whether the application should write to the metadata store |
| K2HB_VALIDATOR_SCHEMA | A json file that encompasses the validator schema, e.g. `business_message.schema.json` |
| K2HB_KAFKA_TOPIC_REGEX | Topics matching this are subscribed to unless they also match the exclusion regex, e.g. `(db[.]{1}[-\w]+[.]{1}[-.\w]+)` |
| K2HB_KAFKA_TOPIC_EXCLUSION_REGEX | Topics matching this pattern are excluded from the subscription, e.g. `(db[.]{1}[-\w]+[.]{1}[-.\w]+)` |
| K2HB_QUALIFIED_TABLE_PATTERN | The regex pattern for getting the table name from the topic, e.g. `\w+\.([-\w]+)\.([-.\w]+)` |
| K2HB_AWS_S3_MANIFEST_DIRECTORY | The name of the directory for the AWS S3 manifest, e.g. `manifest_prefix` |            
| K2HB_TRUSTSTORE_PATH | The SSL truststore location which is needed if Insecure Kafka is not true |         
| K2HB_TRUSTSTORE_PASSWORD | The SSL truststore password which is needed if Insecure Kafka is not true |       
| K2HB_KEYSTORE_PATH | The SSL keystore path which is needed if Insecure Kafka is not true |     
| K2HB_KEYSTORE_PASSWORD | The SSL keystore password which is needed if Insecure Kafka is not true |     
| K2HB_PRIVATE_KEY_PASSWORD | The SSL private key password which is needed if Insecure Kafka is not true |                                                                                                                                                                                            
