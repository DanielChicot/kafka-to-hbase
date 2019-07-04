FROM zenika/kotlin:1.3-jdk8-slim as build

WORKDIR /kafka2hbase

ENV GRADLE "/kafka2hbase/gradlew --no-daemon"

# Copy and generate the gradle wrapper
COPY gradlew .
COPY gradle/ ./gradle
RUN $GRADLE wrapper

# Copy the gradle config and install dependencies
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
RUN $GRADLE build

# Copy the source
COPY src/ ./src

RUN $GRADLE distTar

FROM openjdk:8-slim

ARG VERSION=1.0-SNAPSHOT
ARG DIST=kafka2hbase-$VERSION
ARG DIST_FILE=$DIST.tar

RUN apt-get update && apt-get install -y gosu && rm -rf /var/lib/apt/lists/*

COPY ./entrypoint.sh /

WORKDIR /kafka2hbase

COPY --from=build /kafka2hbase/build/distributions/$DIST_FILE .

RUN tar -xf $DIST_FILE --strip-components=1

ENTRYPOINT ["/entrypoint.sh"]
CMD ["./bin/kafka2hbase"]