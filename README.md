# krampus

This is an experimental stream processing system to test different big data technologies. The current version is built in 
Scala and [Akka](https://github.com/akka/akka). 

# Tech stack

Java 1.8 is the target JVM version.

You need to have [SBT](http://www.scala-sbt.org/download.html) and [Scala](https://www.scala-lang.org/) installed on your system.

Krampus requires SBT 0.13.13 or higher.

Currently Akka based pipeline is the only implementation of this system (see below). 

## Akka based pipeline

* [Apache Kafka](https://kafka.apache.org/) is used as a data ingestion bus.
* [Avro](https://avro.apache.org/) is used as the data binary protocol. 
* [Akka](https://github.com/akka/akka) is used as the toolkit of choice for all event processing components.
* [Spark](https://spark.apache.org/) is used for building machine learning models for anomaly detection.
* [Grafana](https://grafana.com/) is used for visualization.
* [Cassandra](http://cassandra.apache.org/) is used to store the Spark ML models.
* [Docker](https://www.docker.com/) is used to contanerize all these components in order to start all processing elements on one host machine.

# Build docker images ###
    ./build-all.sh 