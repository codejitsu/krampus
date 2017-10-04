# Krampus

This is an experimental stream processing system to test different big data technologies. The current version is built using 
Scala and [Akka](https://github.com/akka/akka). 

# Data source

Wikipedia recent changes IRC channels for different countries - https://meta.wikimedia.org/wiki/IRC/Channels#Raw_feeds

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
* [Play! Framework](https://www.playframework.com/) is the technology the web app is built with.

# Build docker images ###
    ./build-all.sh 
    
# Start Krampus ###
    docker-compose up
    
# Krampus in action ###

Krampus works as follows:

* _krampus-source_ component reads all the Wikipedia recent changes channel and writes the data to the shared channel.
* _krampus-producer_ simply makes 'tail -f' on this shared channel, reads the data, converts Wikipedia data to [Avro](https://avro.apache.org/) binary messages and pushes these messages to the [Apache Kafka](https://kafka.apache.org/) topic 'wikidata'.
* _krampus-metrics-aggregator_ reads all the data from the 'wikidata' Kafka topic, makes aggregations (counters) and writes the stats to the graphite.
* _krampus-processor_ reads all entries from the 'wikidata' Kafka topic and stores every message to Cassandra (this data will be consumed later by the ML module).
* _krampus-web-app_ is a web interface to the pipeline - you can easily look into all wikipedia channels in real time.

After start you should see log lines like this in your terminal:

![Terminal](https://raw.githubusercontent.com/codejitsu/krampus/master/doc/img/command-line.png)

# Important URLs

* [localhost:81](http://localhost:81) - this is your Graphite instance

![Graphite](https://raw.githubusercontent.com/codejitsu/krampus/master/doc/img/graphite.png)
   
* [localhost:80](http://localhost:80) - this is your Grafana instance.
    * Login/password: admin/admin
    * Select _Krampus_ Dashboard
    
      ![Grafana](https://raw.githubusercontent.com/codejitsu/krampus/master/doc/img/grafana.png)
      
    * You should see something like this:
    
      ![Grafana](https://raw.githubusercontent.com/codejitsu/krampus/master/doc/img/grafana-dashboard.png)
      
* [localhost:9000](http://localhost:9000) - if you point your web browser at this URL you should see something like this:

![Web App](https://raw.githubusercontent.com/codejitsu/krampus/master/doc/img/web-app.png)     

This is the krampus web app! You can switch the channel name on the UI or in URL and see all the channel changes in real time!