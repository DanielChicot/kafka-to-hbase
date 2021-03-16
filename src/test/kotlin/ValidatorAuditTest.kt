import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ValidatorAuditTest : StringSpec() {

    init {

        "Audit Schema: Valid message passes validation." {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
            |{
            |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
            |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
            |   "@type" : "V4",
            |   "message" : {
            |       "dbObject" : "xxxxxx",
            |       "encryption" : {
            |           "keyEncryptionKeyId" : "cloudhsm:12,34",
            |           "encryptedEncryptionKey" : "xxxxxx",
            |           "initialisationVector" : "xxxxxxxx=="
            |       },
            |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693+0000",
            |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_id" : {
            |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
            |       }
            |   },
            |   "version" : "core-4.release_147.3",
            |   "timestamp" : "2020-05-21T17:18:15.706+0000"
            |}
            """.trimMargin()
            )
        }

        "Audit Schema: Valid message alternate date format passes validation." {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
            |{
            |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
            |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
            |   "@type" : "V4",
            |   "message" : {
            |       "dbObject" : "xxxxxx",
            |       "encryption" : {
            |           "keyEncryptionKeyId" : "cloudhsm:12,34",
            |           "encryptedEncryptionKey" : "xxxxxx",
            |           "initialisationVector" : "xxxxxxxx=="
            |       },
            |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693",
            |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_id" : {
            |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
            |       }
            |   },
            |   "version" : "core-4.release_147.3",
            |   "timestamp" : "2020-05-21T17:18:15.706"
            |}
            """.trimMargin()
            )
        }

        "Audit Schema: Valid message alternate date format number two passes validation." {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
            |{
            |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
            |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
            |   "@type" : "V4",
            |   "message" : {
            |       "dbObject" : "xxxxxx",
            |       "encryption" : {
            |           "keyEncryptionKeyId" : "cloudhsm:12,34",
            |           "encryptedEncryptionKey" : "xxxxxx",
            |           "initialisationVector" : "xxxxxxxx=="
            |       },
            |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693Z",
            |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_id" : {
            |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
            |       }
            |   },
            |   "version" : "core-4.release_147.3",
            |   "timestamp" : "2020-05-21T17:18:15.706Z"
            |}
            """.trimMargin()
            )
        }

        "Audit Schema: Valid message - UCFS prod sample 1 passes validation" {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
            |{
            |  "traceId" : "59497c68-4e3a-46cd-9215-9e59fe4b22f6",
            |  "unitOfWorkId" : "457e6a1c-c288-497c-a4a2-1e50bef67c90",
            |  "@type" : "V4",
            |  "message" : {
            |    "dbObject" : "xxx",
            |    "encryption" : {
            |      "keyEncryptionKeyId" : "cloudhsm:262152,262151",
            |      "encryptedEncryptionKey" : "xxx",
            |      "encryptionKeyId" : "b4f18de2-86e2-4525-9b39-3130e9a2800f",
            |      "initialisationVector" : "xxxZ"
            |    },
            |    "_lastModifiedDateTime" : "2020-08-05T07:07:39.767+0000",
            |    "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |    "_id" : {
            |      "auditId" : "7b61b79f-80a8-4aed-aa74-212a3c4c4d70"
            |    }
            |  },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:39.768+0000"
            |}
            """.trimMargin()
            )
        }

        "Audit Schema: Valid message - UCFS prod sample 2 passes validation" {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
            |{
            |  "traceId" : "1806eb69-a7be-4ade-8306-00f46e6852c5",
            |  "unitOfWorkId" : "ff041e1f-67bb-4013-a00f-3ea787da4864",
            |  "@type" : "V4",
            |  "message" : {
            |    "dbObject" : "xxx",
            |    "encryption" : {
            |      "keyEncryptionKeyId" : "cloudhsm:262152,262151",
            |      "encryptedEncryptionKey" : "xxx",
            |      "encryptionKeyId" : "ff001d09-0da3-408f-a257-fdcd8052bdcd",
            |      "initialisationVector" : "xxx"
            |    },
            |    "_lastModifiedDateTime" : "2020-08-05T07:07:00.105+0000",
            |    "@type" : "AUDIT",
            |    "_id" : {
            |      "auditId" : "9e181ca2-2d11-4703-928a-841f7be57c17"
            |    }
            |  },
            |  "version" : "core-4.release_152.16",
            |  "timestamp" : "2020-08-05T07:07:00.105+0000"
            |}
            """.trimMargin()
            )
        }

        "Audit Schema: Incorrect _id sub-field fails validation" {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |  "traceId" : "1806eb69-a7be-4ade-8306-00f46e6852c5",
                |  "unitOfWorkId" : "ff041e1f-67bb-4013-a00f-3ea787da4864",
                |  "@type" : "V4",
                |  "message" : {
                |    "dbObject" : "xxx",
                |    "encryption" : {
                |      "keyEncryptionKeyId" : "cloudhsm:262152,262151",
                |      "encryptedEncryptionKey" : "xxx",
                |      "encryptionKeyId" : "ff001d09-0da3-408f-a257-fdcd8052bdcd",
                |      "initialisationVector" : "xxx"
                |    },
                |    "_lastModifiedDateTime" : "2020-08-05T07:07:00.105+0000",
                |    "@type" : "AUDIT",
                |    "_id" : {
                |      "OTHER_ID" : "9e181ca2-2d11-4703-928a-841f7be57c17"
                |    }
                |  },
                |  "version" : "core-4.release_152.16",
                |  "timestamp" : "2020-08-05T07:07:00.105+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: only 1 subschema matches out of 2'."
        }

        "Audit Schema: Missing '#/message/@type' field causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [@type] not found'."
        }

        "Audit Schema: Incorrect '#/message/@type' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693+0000",
                |       "@type" : 1,
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/@type: expected type: String, found: Integer'."
        }

        "Audit Schema: Empty string '#/message/_id/messageId' field causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693+0000",
                |       "@type" : "AUDIT",
                |       "_id" : {
                |           "auditId" : ""
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }

            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: only 1 subschema matches out of 2'."
        }

        "Audit Schema: Incorrect '#/message/_id' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693+0000",
                |       "@type" : "AUDIT",
                |       "_id" : [1, 2, 3, 4, 5, 6, 7 ,8 , 9]
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: only 1 subschema matches out of 2'."
        }

        "Audit Schema: Empty '#/message/_id' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.693+0000",
                |       "@type" : "AUDIT",
                |       "_id" : {}
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_id: #: only 1 subschema matches out of 2'."
        }

        "Audit Schema: Missing '#/message/_lastModifiedDateTime' does not cause validation failure." {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
            )
        }

        "Audit Schema: Null '#/message/_lastModifiedDateTime' does not cause validation failure." {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
			|{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
			|   "message" : {
			|       "dbObject" : "xxxxxx",
			|       "encryption" : {
			|           "keyEncryptionKeyId" : "cloudhsm:12,34",
			|           "encryptedEncryptionKey" : "xxxxxx",
			|           "initialisationVector" : "xxxxxxxx=="
			|       },
			|       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_lastModifiedDateTime" : null,
			|       "_id" : {
			|           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
			|       }
			|   },
			|   "version" : "core-4.release_147.3",
			|   "timestamp" : "2020-05-21T17:18:15.706+0000"
			|}
			""".trimMargin()
            )
        }

        "Audit Schema: Empty '#/message/_lastModifiedDateTime' does not cause validation failure." {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
			|{
			|   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
			|   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
			|   "@type" : "V4",
			|   "message" : {
			|       "dbObject" : "xxxxxx",
			|       "encryption" : {
			|           "keyEncryptionKeyId" : "cloudhsm:12,34",
			|           "encryptedEncryptionKey" : "xxxxxx",
			|           "initialisationVector" : "xxxxxxxx=="
			|       },
			|       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_lastModifiedDateTime" : "",
			|       "_id" : {
			|           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
			|       }
			|   },
			|   "version" : "core-4.release_147.3",
			|   "timestamp" : "2020-05-21T17:18:15.706+0000"
			|}
			""".trimMargin()
            )
        }


        "Audit Schema: Incorrect '#/message/_lastModifiedDateTime' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : 42,
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_lastModifiedDateTime: #: no subschema matched out of the total 2 subschemas'."
        }

        "Audit Schema: Incorrect '#/message/_lastModifiedDateTime' format causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2013-03-13",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/_lastModifiedDateTime: #: no subschema matched out of the total 2 subschemas'."
        }

        "Audit Schema: Missing '#/message/dbObject' field causes validation failure." {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [dbObject] not found'."
        }

        "Audit Schema: Incorrect '#/message/dbObject' type  causes validation failure." {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : {},
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/dbObject: expected type: String, found: JSONObject'."
        }

        "Audit Schema: Empty '#/message/dbObject' causes validation failure." {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/dbObject: expected minLength: 1, actual: 0'."
        }

        "Audit Schema: Missing '#/message/encryption' causes validation failure." {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message: required key [encryption] not found'."
        }

        "Audit Schema: Incorrect '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : "abc",
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: expected type: JSONObject, found: String'."
        }

        "Audit Schema: Missing keyEncryptionKeyId from '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: required key [keyEncryptionKeyId] not found'."
        }

        "Audit Schema: Missing initialisationVector from '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx"
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: required key [initialisationVector] not found'."
        }

        "Audit Schema: Missing encryptedEncryptionKey from '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption: required key [encryptedEncryptionKey] not found'."
        }

        "Audit Schema: Empty keyEncryptionKeyId from '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/keyEncryptionKeyId: string [] does not match pattern ^cloudhsm:\\d+,\\d+$'."
        }

        "Audit Schema: Empty initialisationVector from '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : ""
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/initialisationVector: expected minLength: 1, actual: 0'."
        }

        "Audit Schema: Empty encryptedEncryptionKey from '#/message/encryption' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/encryptedEncryptionKey: expected minLength: 1, actual: 0'."
        }

        "Audit Schema: Incorrect '#/message/encryption/keyEncryptionKeyId' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : 42,
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/keyEncryptionKeyId: expected type: String, found: Integer'."
        }

        "Audit Schema: Incorrect initialisationVector '#/message/encryption/initialisationVector' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : {}
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/initialisationVector: expected type: String, found: JSONObject'."
        }

        "Audit Schema: Incorrect '#/message/encryption/encryptedEncryptionKey' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : ["answer", 42],
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
                )
            }
            exception.message shouldBe "Message failed schema validation: '#/message/encryption/encryptedEncryptionKey: expected type: String, found: JSONArray'."
        }

        "Audit Schema: Incorrect keyEncryptionKeyId '#/message/encryption/keyEncryptionKeyId' type causes validation failure." {
            TestUtils.auditMessageValidator()

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
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
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

        "Audit Schema: '#/message/unitOfWorkId' is required" {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                |{
                |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
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

        "Audit Schema: '#/message/unitOfWorkId' can be null" {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
            |{
            |   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
            |   "unitOfWorkId" : null,
            |   "@type" : "V4",
            |   "message" : {
            |       "dbObject" : "xxxxxx",
            |       "encryption" : {
            |           "keyEncryptionKeyId" : "cloudhsm:12,34",
            |           "encryptedEncryptionKey" : "xxxxxx",
            |           "initialisationVector" : "xxxxxxxx=="
            |       },
            |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
            |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
            |       "_id" : {
            |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
            |       }
            |   },
            |   "version" : "core-4.release_147.3",
            |   "timestamp" : "2020-05-21T17:18:15.706+0000"
            |}
            """.trimMargin()
            )
        }

        "Audit Schema: '#/message/traceId' is required" {
            TestUtils.auditMessageValidator()

            val exception = shouldThrow<InvalidMessageException> {
                Validator().validate(
                    """
                    |{
                    |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                    |   "@type" : "V4",
                    |   "message" : {
                    |       "dbObject" : "xxxxxx",
                    |       "encryption" : {
                    |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                    |           "encryptedEncryptionKey" : "xxxxxx",
                    |           "initialisationVector" : "xxxxxxxx=="
                    |       },
                    |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                    |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                    |       "_id" : {
                    |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
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

        "Audit Schema: '#/message/traceId' can be null" {
            TestUtils.auditMessageValidator()

            Validator().validate(
                """
                |{
                |   "traceId" : null,
                |   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                |   "@type" : "V4",
                |   "message" : {
                |       "dbObject" : "xxxxxx",
                |       "encryption" : {
                |           "keyEncryptionKeyId" : "cloudhsm:12,34",
                |           "encryptedEncryptionKey" : "xxxxxx",
                |           "initialisationVector" : "xxxxxxxx=="
                |       },
                |       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                |       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                |       "_id" : {
                |           "auditId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                |       }
                |   },
                |   "version" : "core-4.release_147.3",
                |   "timestamp" : "2020-05-21T17:18:15.706+0000"
                |}
                """.trimMargin()
            )
        }

    } //end init
}
