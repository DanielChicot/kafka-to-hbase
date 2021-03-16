# Local development

You will need local installs of Docker, Gradle and Kotlin, and so a JVM on at least 1.8.
The SDK-Man utility is good for package management of these.

## When docker runs out of space

...you may see erronious erors that are not obvious.

For example, the Integration test may spin waiting for MySQL to start up. 
Check the MySQL container logs, which may report:
   ```
   [ERROR] --initialize specified but the data directory has files in it. Aborting.
   ```

For this sort of thing, it's usually the docker volume flling up; try and run
   ```
   docker system prune --volumes
   ```



## Makefile

A Makefile wraps some of the gradle and docker-compose commands to give a
more unified basic set of operations. These can be checked by running:

   ```
   make help
   ```

## Local Jar Build

Ensure a JVM is installed and run the gradle build.

   ```
   make local-build
   ```

## Run local unit tests

The unit tests use JUnit to run and are written using specification language.
They can be executed with the following command.

   ```
   make local-test
   ```

## Create local Distribution

If a standard zip file is required, just use the assembleDist command.
This produces a zip and a tarball of the latest version.
   ```
   make local-dist
   ```

## Build full local stack

You can build all the local images with
   ```
   make build
   ```

## Push local images into AWS DEV account

You will need to know your AWS account number, have relevant permssions and create a ECR in advance, i.e. "k2hb-test"

Then you can push to dev like this;
   ```
   make push-local-to-ecr aws_dev_account=12345678 temp_image_name=k2hb-test aws_default_region=eu-middle-3
   ```

Which does the following steps for you
   ```
   export AWS_DEV_ACCOUNT=12345678
   export TEMP_IMAGE_NAME=k2hb-test
   export AWS_DEFAULT_REGION=eu-middle-3
   aws ecr get-login-password --region ${AWS_DEFAULT_REGION} --profile dataworks-development | docker login --username AWS --password-stdin ${AWS_DEV_ACCOUNT}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com
   docker tag kafka2hbase ${AWS_DEV_ACCOUNT}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${TEMP_IMAGE_NAME}
   docker push ${AWS_DEV_ACCOUNT}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${TEMP_IMAGE_NAME}
   ```

## Run full local stack

A full local stack can be run using the provided Dockerfile and Docker
Compose configuration. The Dockerfile uses a multi-stage build so no
pre-compilation is required.

   ```
   make up
   ```

The environment can be stopped without losing any data:

   ```
   make down
   ```

Or completely removed including all data volumes:

   ```
   make destroy
   ```

## Run integration tests

Integration tests can be executed inside a Docker container to make use of
the Kafka and Hbase instances running in the local stack. The integration
tests are written in Kotlin and use the standard `kotlintest` testing framework.

To run from a clean build:

   ```
   make integration-all
   ```

To run just the tests again with everything running

   ```
   make integration-tests
   ```

## Run in an IDE

Both Kafka2HBase and the integration tests can be run in an IDE to facilitate
quicker feedback then a containerized approach. This is useful during active development.

To do this first bring up the hbase, kafka and zookeeper containers:

   ```
   make services
   ```

On the run configuration for Kafka2Hbase set the following environment variables
(nb not system properties)

   ```
   K2HB_HBASE_ZOOKEEPER_QUORUM=localhost;K2HB_KAFKA_POLL_TIMEOUT=PT2S
   ```

And on the run configuration for the integration tests set these:

   ```
   K2HB_KAFKA_BOOTSTRAP_SERVERS=localhost:9092;K2HB_HBASE_ZOOKEEPER_QUORUM=localhost
   ```

Then insert into your local hosts file the names, IP addresses of the kafka and
hbase containers:

   ```
   ./hosts.sh
   ```

## Getting logs

The services are listed in the `docker-compose.yaml` file and logs can be
retrieved for all services, or for a subset.

   ```
   docker-compose logs hbase
   ```

The logs can be followed so new lines are automatically shown.

   ```
   docker-compose logs -f hbase
   ```

## Getting an HBase shell

To access the HBase shell it's necessary to use a Docker container. This
can be run as a separate container.

   ```
   make hbase-shell
   ```
