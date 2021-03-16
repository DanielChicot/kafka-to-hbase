SHELL=bash
S3_READY_REGEX=^Ready\.$
RDBMS_READY_REGEX='mysqld: ready for connections'
aws_dev_account=NOT_SET
temp_image_name=NOT_SET
aws_default_region=NOT_SET

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: bootstrap
bootstrap: ## Bootstrap local environment for first use
	make git-hooks

.PHONY: git-hooks
git-hooks: ## Set up hooks in .git/hooks
	@{ \
		HOOK_DIR=.git/hooks; \
		for hook in $(shell ls .githooks); do \
			if [ ! -h $${HOOK_DIR}/$${hook} -a -x $${HOOK_DIR}/$${hook} ]; then \
				mv $${HOOK_DIR}/$${hook} $${HOOK_DIR}/$${hook}.local; \
				echo "moved existing $${hook} to $${hook}.local"; \
			fi; \
			ln -s -f ../../.githooks/$${hook} $${HOOK_DIR}/$${hook}; \
		done \
	}

hbase-up: ## Bring up and provision zookeeper and hbase
	docker-compose -f docker-compose.yaml up -d zookeeper hbase
	@{ \
		echo Waiting for hbase.; \
		while ! docker logs hbase 2>&1 | grep "Master has completed initialization" ; do \
			sleep 2; \
			echo Waiting for hbase.; \
		done; \
		sleep 5; \
		echo ...hbase ready.; \
	}

rdbms: ## Bring up and provision mysql
	docker-compose -f docker-compose.yaml up -d metadatastore
	@{ \
		while ! docker logs metadatastore 2>&1 | grep "^Version" | grep 3306; do \
			echo Waiting for metadatastore.; \
			sleep 2; \
		done; \
		sleep 5; \
	}
	docker exec -i metadatastore mysql --user=root --password=password metadatastore < ./docker/metadatastore/create_table.sql
	docker exec -i metadatastore mysql --user=root --password=password metadatastore < ./docker/metadatastore/grant_user.sql

prometheus:
	docker-compose up -d prometheus

pushgateway:
	docker-compose up -d pushgateway

services: hbase-up rdbms prometheus pushgateway ## Bring up supporting services in docker
	docker-compose -f docker-compose.yaml up --build -d kafka aws-s3
	@{ \
		while ! docker logs aws-s3 2> /dev/null | grep -q $(S3_READY_REGEX); do \
			echo Waiting for s3.; \
			sleep 2; \
		done; \
	}
	docker-compose up --build s3-provision
	docker-compose up --build -d kafka2s3


mysql_root: ## Get a client session on the metadatastore database.
	docker exec -it metadatastore mysql --user=root --password=password metadatastore

mysql_k2hbwriter: ## Get a client session on the metadatastore database.
	docker exec -it metadatastore mysql --user=k2hbwriter --password=password metadatastore

up: services ## Bring up Kafka2Hbase in Docker with supporting services
	docker-compose -f docker-compose.yaml up --build -d kafka2hbase kafka2hbaseequality

integration-tests:
	docker-compose -f docker-compose.yaml run --name integration-test integration-test gradle --no-daemon \
		--rerun-tasks integration-test integration-test-equality integration-load-test

build-base: ## Build the base images which certain images extend.
	@{ \
		pushd docker; \
		docker build --tag dwp-java:latest --file .java/Dockerfile . ; \
		docker build --tag dwp-python-preinstall:latest --file ./python/Dockerfile . ; \
		cp ../settings.gradle.kts ../gradle.properties . ; \
		docker build --tag dwp-kotlin-slim-gradle-k2hb:latest --file ./gradle/Dockerfile . ; \
		rm -rf settings.gradle.kts gradle.properties ; \
		popd; \
	}

delete-topics: ## Delete a topic
	docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181 --delete --topic '^(db.+|test-dlq-topic)'
	make list-topics

list-topics: ## List the topics
	docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181 --list

restart-prometheus:
	docker stop prometheus pushgateway
	docker rm prometheus pushgateway
	docker-compose build prometheus
	docker-compose up -d prometheus pushgateway
