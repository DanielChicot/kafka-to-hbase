import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    id( "com.github.ben-manes.versions") version "0.36.0"
    application
}

group = "uk.gov.dwp.dataworks"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.amazonaws:aws-java-sdk-core:1.11.955")
    implementation("com.amazonaws:aws-java-sdk-s3:1.11.955")
    implementation("com.amazonaws:aws-java-sdk-secretsmanager:1.11.955")
    implementation("com.beust:klaxon:5.4")
    implementation("com.github.dwp:dataworks-common-logging:0.0.6")
    implementation("com.github.everit-org.json-schema:org.everit.json.schema:1.12.2")
    implementation("com.google.protobuf:protobuf-java:2.5.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("io.prometheus:simpleclient:0.10.0")
    implementation("io.prometheus:simpleclient_logback:0.10.0")
    implementation("io.prometheus:simpleclient_httpserver:0.10.0")
    implementation("io.prometheus:simpleclient_pushgateway:0.10.0")
    implementation("mysql:mysql-connector-java:6.0.6")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.apache.hbase:hbase-client:1.4.9")
    implementation("org.apache.kafka:kafka-clients:2.7.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.30")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.4.1")
    testImplementation("io.kotest:kotest-assertions-json:4.4.1")
    testImplementation("io.kotest:kotest-property-jvm:4.4.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.4.1")
    testImplementation("io.ktor:ktor-client-apache:1.5.1")
    testImplementation("io.ktor:ktor-client-core:1.5.1")
    testImplementation("io.ktor:ktor-client-gson:1.5.1")
    testImplementation("io.mockk:mockk:1.10.6")
    testImplementation("mysql:mysql-connector-java:6.0.6")
    testImplementation("org.mockito:mockito-core:3.7.7")
}

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
}

application {
    mainClassName = "Kafka2HbaseKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

sourceSets {
    create("integration") {
        java.srcDir(file("src/integration/kotlin"))
        compileClasspath += sourceSets.getByName("main").output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
    create("unit") {
        java.srcDir(file("src/test/kotlin"))
        compileClasspath += sourceSets.getByName("main").output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<Test>("integration-test") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    filter {
        includeTestsMatching("Kafka2hbUcfsIntegrationSpec*")
    }
    environment("K2HB_RETRY_INITIAL_BACKOFF", "1")
    environment("K2HB_RETRY_MAX_ATTEMPTS", "3")
    environment("K2HB_RETRY_BACKOFF_MULTIPLIER", "1")

    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT)
    }
}

tasks.register<Test>("integration-test-equality") {
    description = "Runs the integration tests for equality schema"
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    filter {
        includeTestsMatching("Kafka2hbEqualityIntegrationSpec*")
    }
    //copy all env vars from unix/your integration container into the test
    setEnvironment(System.getenv())
    environment("K2HB_RETRY_INITIAL_BACKOFF", "1")
    environment("K2HB_RETRY_MAX_ATTEMPTS", "3")
    environment("K2HB_RETRY_BACKOFF_MULTIPLIER", "1")
    environment("K2HB_VALIDATOR_SCHEMA", "equality_message.schema.json")
    environment("K2HB_WRITE_TO_METADATA_STORE", "true")
    environment("K2HB_QUALIFIED_TABLE_PATTERN", """([-\w]+)\.([-\w]+)""")
    environment("K2HB_KAFKA_TOPIC_REGEX", """^(data[.][-\w]+)$""")

    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT)
    }
}


tasks.register<Test>("integration-load-test") {
    description = "Runs the integration load tests"
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    filter {
        includeTestsMatching("Kafka2hbIntegrationLoadSpec*")
    }

    //copy all env vars from unix/your integration container into the test
    setEnvironment(System.getenv())
    environment("K2HB_RETRY_INITIAL_BACKOFF", "1")
    environment("K2HB_RETRY_MAX_ATTEMPTS", "3")
    environment("K2HB_RETRY_BACKOFF_MULTIPLIER", "1")
    environment("K2HB_USE_AWS_SECRETS", "false")
    environment("K2HB_WRITE_TO_METADATA_STORE", "true")

    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT)
    }
}

tasks.register<Test>("unit") {
    description = "Runs the unit tests"
    group = "verification"
    testClassesDirs = sourceSets["unit"].output.classesDirs
    classpath = sourceSets["unit"].runtimeClasspath

    environment("K2HB_RETRY_INITIAL_BACKOFF", "1")
    environment("K2HB_RETRY_MAX_ATTEMPTS", "3")
    environment("K2HB_RETRY_BACKOFF_MULTIPLIER", "1")
    environment("K2HB_USE_AWS_SECRETS", "false")
    environment("K2HB_WRITE_TO_METADATA_STORE", "true")

    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

