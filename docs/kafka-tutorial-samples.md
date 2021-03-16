# Sample 101 Kafka command lines for practice

## See also

| **Topic** | **Type** | **Link to Document** |
| --------- | -------- | -------------------- |
| K2hb: Kafka, Broker and Consumer, Kotlin | Pubic Git Repo | https://github.com/dwp/kafka-to-hbase.git  | 
| Reconciliation, Kotlin | Public Git Repo | https://github.com/dwp/kafka-to-hbase-reconciliation.git  | 
| Kafka | All-About | https://www.tutorialspoint.com/apache_kafka/apache_kafka_fundamentals.htm  | 
| Kafka Command Line | Tutorial | https://docs.cloudera.com/documentation/kafka/latest/topics/kafka_command_line.html  | 

## Basic examples

### Bring up all the service containers and get a shell in the kafka box

   ```shell script
   make services
   # or
   make integration-all
   ```

then 

   ```shell script
   docker exec -it kafka sh
   ```
or
   ```shell script
   make kafka-shell
   ```

### Inside the shell, find all the utility scripts

   ```shell script
   cd /opt/kafka/bin
   ls
   ```

or from in your machine, not the container
   ```shell script
   make kafka-shell-bin
   ```

### Check the current list of topics

   ```shell script
   ./kafka-topics.sh --zookeeper zookeeper:2181 --list
   ```
or
   ```shell script
   make tutorial-list-all
   ```

### Make a new topic

...note that doing it this way we must specify the partitions, while through code it is defaulted at the server level.

   ```shell script
   ./kafka-topics.sh --create --topic my-topic --zookeeper zookeeper:2181 --replication-factor 1 --partitions 20
   ```

or if it might already exist

   ```shell script
   ./kafka-topics.sh --if-not-exists --create --topic my-topic --zookeeper zookeeper:2181 --replication-factor 1 --partitions 20
   ```
or
   ```shell script
   make tutorial-create-topic
   ```

### Describe the new topic

   ```shell script
   ./kafka-topics.sh --describe --topic my-topic --zookeeper zookeeper:2181
   ```
or
   ```shell script
   make tutorial-describe-topic
   ```

### Publish to Topic 

This starts an interactive prompt, these are separated by you hitting Return

...note that this interacts with the Broker rather than going through it to ZooKeeper

   ```shell script
   ./kafka-console-producer.sh --broker-list localhost:9092 --topic my-topic
   ```
or
   ```shell script
   make tutorial-publish-simple
   ```

### Subscribe to Topic

...note that this interacts with the Broker rather than going through it to ZooKeeper

   ```shell script
   ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my-topic --from-beginning --group my-consumer-group
   ```
or
   ```shell script
   make tutorial-subscribe-by-group
   ```


## Advanced samples

### Publish with keys and consume by partition.

The `key` a message is put onto a topic with will determine the `partition` it is put into. This is deterministic. 
Thus, it is important to have well-hashed keys to get an even partition spread.

Here we will start multiple terminals and watch messages be multiplexed between consumers. 
In real code we do not specify the partitions, they are assigned by the Broker to each consumer in the group.
This next example uses fixed partition consumers as a proxy for this.

#### Start in a fresh terminal

1. Run `make tutorial-list-all` to show all the topics thus far created.
1. Run `make tutorial-list-topic tutorial_topic=my-multi` to show this one is not there.
1. Run `make tutorial-create-topic tutorial_topic=my-multi tutorial_partition=2` to create a new one with partitions 0 and 1.
1. Run `make tutorial-describe-topic tutorial_topic=my-multi` to create a new one with partitions 0 and 1.

### In a second terminal

1. Run `make tutorial-subscribe-by-partition tutorial_topic=my-multi tutorial_partition=0` subscribe to partition 0.
1. Leave this running.

### In a third terminal

1. Run `make tutorial-subscribe-by-partition tutorial_topic=my-multi tutorial_partition=1` subscribe to partition 1.
1. Leave this running.

### Back in the first terminal

1. Run `make tutorial-publish-with-key tutorial_topic=my-multi` to publish to the topic.
1. This will open a terminal prompt `>`.
1. Enter values in the form `key:value`
1. You will see that a given key always goes to the same partition consumer in the other terminals.
1. For example, `a-key:value-1` to partition `0` and `b-key:value-2` to partition `1`.

### In terminal two

1. Stop the consumer with CTRL-C
1. Run `make tutorial-subscribe-by-group tutorial_topic=my-multi`
1. Observe as it is currently the only consumer in the group, it takes all the messages.

### In terminal three

1. Stop the consumer with CTRL-C
1. Run `make tutorial-subscribe-by-group tutorial_topic=my-multi`
1. Now both consumers are in one group.
1. As they between them are now at the Head Offset, the second consumer receives no messages.

### Back in the first terminal

1. Publish new messages in the form `key:value`
1. You will see that a given key always goes to the same consumer in the other terminals.
1. This is because with two consumers each is assigned an equal share of the partitions by the Lead Broker.
1. For example, `a-key:value-1` to partition `0` and `b-key:value-2` to partition `1`.
