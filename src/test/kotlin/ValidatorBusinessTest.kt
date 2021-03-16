import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ValidatorBusinessTest : StringSpec() {

    init {
        "Default business schema: Valid message passes validation." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": {
            |           "declarationId": 1
            |       },
            |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
            |       "collection" : "addresses",
            |       "db": "core",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: Valid message with alternate date format passes validation." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": {
            |           "declarationId": 1
            |       },
            |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104",
            |       "collection" : "addresses",
            |       "db": "core",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: Valid message with alternate date format number two passes validation." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": {
            |           "declarationId": 1
            |       },
            |       "_lastModifiedDateTime": "2017-06-19T23:00:10.875Z",
            |       "collection" : "addresses",
            |       "db": "core",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: Additional properties allowed." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": {
            |           "declarationId": 1
            |       },
            |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
            |       "collection" : "addresses",
            |       "db": "core",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "==",
            |           "additional": [0, 1, 2, 3, 4]
            |       },
            |       "additional": [0, 1, 2, 3, 4]
            |   },
            |   "additional": [0, 1, 2, 3, 4],
            |   "version" : "core-4.release_152.16",
            |   "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: Missing '#/message' causes validation failure." {
            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "msg": [0, 1, 2],
                |   "version" : "core-4.release_152.16",
                |   "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#: required key [message] not found'."
        }

        "Default business schema: Incorrect '#/message' type causes validation failure." {
            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": 123,
                |   "version" : "core-4.release_152.16",
                |   "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: expected type: JSONObject, found: Integer'."
        }

        "Default business schema: Missing '#/message/@type' field causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                    |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "collection" : "addresses",
                |       "db": "core",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [@type] not found'."
        }

        "Default business schema: Incorrect '#/message/@type' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "@type": 1,
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "collection" : "addresses",
                |       "db": "core",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/@type: expected type: String, found: Integer'."
        }

        "Default business schema: String '#/message/_id' field does not cause validation failure." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": "abcdefg",
            |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
            |       "db": "abcd",
            |       "collection" : "addresses",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: Integer '#/message/_id' field does not cause validation failure." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": 12345,
            |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
            |       "db": "abcd",
            |       "collection" : "addresses",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }


        "Default business schema: Empty string '#/message/_id' field causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": "",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "db": "abcd",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }

            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: no subschema matched out of the total 3 subschemas'."
        }

        "Default business schema: Incorrect '#/message/_id' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": [1, 2, 3, 4, 5, 6, 7 ,8 , 9],
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "db": "abcd",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: no subschema matched out of the total 3 subschemas'."
        }

        "Default business schema: Empty '#/message/_id' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
            |{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": {},
            |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
            |       "db": "abcd",
            |       "collection" : "addresses",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: no subschema matched out of the total 3 subschemas'."
        }

        "Default business schema: Missing '#/message/_lastModifiedDateTime' does not cause validation failure." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
            |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
            |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
            |   "@type" : "V4",
            |   "message": {
            |       "@type": "hello",
            |       "_id": { part: 1},
            |       "db": "abcd",
            |       "collection" : "addresses",
            |       "dbObject": "asd",
            |       "encryption": {
            |           "keyEncryptionKeyId": "cloudhsm:7,14",
            |           "initialisationVector": "iv",
            |           "encryptedEncryptionKey": "=="
            |       }
            |   },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: Null '#/message/_lastModifiedDateTime' does not cause validation failure." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": null,
                |       "collection" : "addresses",
                |       "db": "core",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
            """.trimMargin()
            )
        }

        "Default business schema: Empty '#/message/_lastModifiedDateTime' does not cause validation failure." {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "",
                |       "collection" : "addresses",
                |       "db": "core",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
            """.trimMargin()
            )
        }


        "Default business schema: Incorrect '#/message/_lastModifiedDateTime' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": { part: 1},
                |       "_lastModifiedDateTime": 12,
                |       "db": "abcd",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
            """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_lastModifiedDateTime: #: no subschema matched out of the total 2 subschemas'."
        }

        "Default business schema: Incorrect '#/message/_lastModifiedDateTime' format causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": { part: 1},
                |       "_lastModifiedDateTime": "2019-07-04",
                |       "db": "abcd",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_lastModifiedDateTime: #: no subschema matched out of the total 2 subschemas'."
        }

        "Default business schema: Missing '#/message/db' field causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [db] not found'."
        }

        "Default business schema: Incorrect '#/message/db' type  causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": [0, 1, 2],
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/db: expected type: String, found: JSONArray'."
        }

        "Default business schema: Empty '#/message/db' causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "collection" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/db: expected minLength: 1, actual: 0'."
        }

        "Default business schema: Missing '#/message/collection' field causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "db" : "addresses",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [collection] not found'."
        }

        "Default business schema: Incorrect '#/message/collection' type  causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "db" : "addresses",
                |       "collection": 5,
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/collection: expected type: String, found: Integer'."
        }

        "Default business schema: Empty '#/message/collection' causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "asd",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/collection: expected minLength: 1, actual: 0'."
        }


        "Default business schema: Missing '#/message/dbObject' field causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "db" : "addresses",
                |       "collection": "core",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [dbObject] not found'."
        }

        "Default business schema: Incorrect '#/message/dbObject' type  causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "db" : "addresses",
                |       "collection": "collection",
                |       "dbObject": { "key": "value" },
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/dbObject: expected type: String, found: JSONObject'."
        }

        "Default business schema: Empty '#/message/dbObject' causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/dbObject: expected minLength: 1, actual: 0'."
        }

        "Default business schema: Missing '#/message/encryption' causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123"
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [encryption] not found'."
        }

        "Default business schema: Incorrect '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": "hello"
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: expected type: JSONObject, found: String'."
        }

        "Default business schema: Missing keyEncryptionKeyId from '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: required key [keyEncryptionKeyId] not found'."
        }

        "Default business schema: Missing initialisationVector from '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:1,2",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: required key [initialisationVector] not found'."
        }

        "Default business schema: Missing encryptedEncryptionKey from '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv"
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: required key [encryptedEncryptionKey] not found'."
        }

        "Default business schema: Empty keyEncryptionKeyId from '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/keyEncryptionKeyId: string [] does not match pattern ^cloudhsm:\\d+,\\d+\$'."
        }

        "Default business schema: Empty initialisationVector from '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:1,2",
                |           "initialisationVector": "",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/initialisationVector: expected minLength: 1, actual: 0'."
        }

        "Default business schema: Empty encryptedEncryptionKey from '#/message/encryption' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": ""
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/encryptedEncryptionKey: expected minLength: 1, actual: 0'."
        }

        "Default business schema: Incorrect '#/message/encryption/keyEncryptionKeyId' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": 0,
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": "=="
                |       }
                |   },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/keyEncryptionKeyId: expected type: String, found: Integer'."
        }

        "Default business schema: Incorrect initialisationVector '#/message/encryption/initialisationVector' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:1,2",
                |           "initialisationVector": {},
                |           "encryptedEncryptionKey": "=="
                |       }
                |  },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/initialisationVector: expected type: String, found: JSONObject'."
        }

        "Default business schema: Incorrect '#/message/encryption/encryptedEncryptionKey' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message": {
                |       "@type": "hello",
                |       "_id": {
                |           "declarationId": 1
                |       },
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                |       "dbObject": "123",
                |       "encryption": {
                |           "keyEncryptionKeyId": "cloudhsm:7,14",
                |           "initialisationVector": "iv",
                |           "encryptedEncryptionKey": [0, 1, 2]
                |       }
                |  },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/encryptedEncryptionKey: expected type: String, found: JSONArray'."
        }

        "Default business schema: Incorrect keyEncryptionKeyId '#/message/encryption/keyEncryptionKeyId' type causes validation failure." {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:aaa,bbbb",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "db": "address",
                |       "collection": "collection",
                |       "_id" : {
                |           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/keyEncryptionKeyId: string [cloudhsm:aaa,bbbb] does not match pattern ^cloudhsm:\\d+,\\d+$'."
        }

        "Default business schema: '#/message/unitOfWorkId' is required" {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:7,14",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "db": "address",
                |       "collection": "collection",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#: required key [unitOfWorkId] not found'."
        }

        "Default business schema: '#/message/unitOfWorkId' can be null" {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
            |{
            |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
            |   "unitOfWorkId" : null,
            |   "@type" : "V4",
            |   "message" : {
            |       "dbObject" : "xxxxxx",
            |       "encryption" : {
            |           "keyEncryptionKeyId" : "cloudhsm:7,14",
            |           "encryptedEncryptionKey" : "xxxxxx",
            |           "initialisationVector" : "xxxxxxxx=="
            |       },
            |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
            |       "db": "address",
            |       "collection": "collection",
            |       "_id" : {
            |           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
            |       }
            |   },
            |   "version" : "core-4.release_147.3",
            |   "timestamp" : "2020-05-21T17:18:15.706+0000"
            |}
            """.trimMargin()
            )
        }

        "Default business schema: '#/message/traceId' is required" {
            TestUtils.defaultMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                    |{
                    |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                    |   "@type" : "V4",
                    |   "message" : {
                    |       "dbObject" : "xxxxxx",
                    |       "encryption" : {
                    |           "keyEncryptionKeyId" : "cloudhsm:7,14",
                    |           "encryptedEncryptionKey" : "xxxxxx",
                    |           "initialisationVector" : "xxxxxxxx=="
                    |       },
                    |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                    |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                    |       "db": "address",
                    |       "collection": "collection",
                    |       "_id" : {
                    |           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                    |       }
                    |   },
                    |   "version" : "core-4.release_147.3",
                    |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                    |}
                    """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#: required key [traceId] not found'."
        }

        "Default business schema: '#/message/traceId' can be null" {
            TestUtils.defaultMessageValidator()

            Validator().validate(
                """
                |{
                |   "traceId" : null,
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:7,14",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "db": "address",
                |       "collection": "collection",
                |       "_id" : {
                |           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
            )
        }

    }  //end init
}
