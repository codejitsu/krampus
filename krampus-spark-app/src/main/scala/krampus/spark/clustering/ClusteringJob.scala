// Copyright (C) 2016, codejitsu.

package krampus.spark.clustering

import com.datastax.spark.connector._
import com.typesafe.scalalogging.LazyLogging
import krampus.spark.util.AppConfig
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

class ClusteringJob(context: SparkContext, config: AppConfig) extends LazyLogging {
  val keyspace = "wiki"
  val table = "edits"

  def distance(a: Vector, b: Vector): Double =
    math.sqrt(a.toArray.zip(b.toArray).map(p => p._1 - p._2).map(d => d * d).sum)

  def distToCentroid(datum: Vector, model: KMeansModel): Double = {
    val cluster = model.predict(datum)
    val centroid = model.clusterCenters(cluster)
    distance(centroid, datum)
  }

  def clusteringScore(data: RDD[Vector], k: Int): Double = {
    val kmeans = new KMeans()
    kmeans.setK(k)

    val model = kmeans.run(data)
    data.map(datum => distToCentroid(datum, model)).mean()
  }

  def run(week: Int, channel: String, modelPath: String): Unit = {
    val editsTable = context.cassandraTable(keyspace, table).cache()
    val wikiData = editsTable.where(s"channel = '#$channel.wikipedia'").cache()

    val weeklyData = wikiData.where(s"week = $week").select("year", "month", "week", "weekday", "day_minute", "id").cache()
    val weeklyEdits =  weeklyData.map(r => ((r.getInt("weekday"), r.getInt("day_minute")), 1)).cache()
    val groupedByMinute = weeklyEdits.reduceByKey(_ + _).map(r => (r._1._2, r._2)).cache()
    val visits = groupedByMinute.map(r => Vectors.dense(r._2)).cache()

    val kmeans = new KMeans()
    kmeans.setK(config.kMeans)
    kmeans.setEpsilon(1.0e-6)

    val model = kmeans.run(visits)
    model.save(context, s"$modelPath/model-week$week-$channel")
    logger.info(s"Model created: $modelPath/model-week$week-$channel")
  }
}

object ClusteringJob extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val appConfig = new AppConfig("spark-app")

    logger.info(appConfig.config.root().render())

    val week = appConfig.targetWeek
    val channel = appConfig.targetChannel
    val modelDirectory = appConfig.modelDirectory
    val cassandraHost = appConfig.cassandraHost

    val conf = new SparkConf()
      .setAppName("KrampusClusteringJob")
      .set("spark.cassandra.connection.host", cassandraHost)
      .setMaster("local")

    val context = new SparkContext(conf)

    val job = new ClusteringJob(context, appConfig)
    job.run(week, channel, modelDirectory)
    context.stop()
  }
}
