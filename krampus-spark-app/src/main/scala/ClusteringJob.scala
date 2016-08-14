// Copyright (C) 2016, codejitsu.

package krampus.spark.clustering

import com.datastax.spark.connector._
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD

class ClusteringJob(context: SparkContext) {
  val keyspace = "wiki"
  val table = "edits"
  val modelTable = "model"

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

  def run(week: String, channel: String, modelPath: String): Unit = {
    val editsTable = context.cassandraTable(keyspace, table).cache()
    val wikiData = editsTable.where(s"channel = '$channel'").cache()

    val weeklyData = wikiData.where(s"week = $week").select("year", "month", "week", "weekday", "day_minute", "id").cache()
    val weeklyEdits =  weeklyData.map(r => ((r.getInt("weekday"), r.getInt("day_minute")), 1)).cache()
    val groupedByMinute = weeklyEdits.reduceByKey(_ + _).map(r => (r._1._2, r._2)).cache()
    val visits = groupedByMinute.map(r => Vectors.dense(r._2)).cache()

    val kmeans = new KMeans()
    kmeans.setK(55)
    kmeans.setEpsilon(1.0e-6)

    val model = kmeans.run(visits)
    model.save(context, modelPath)
  }
}

object ClusteringJob {
  def main(args: Array[String]): Unit = {
    val week = args(0)
    val channel = args(1)
    val cassandraHost = args(2)
    val modelPath = args(3)

    val conf = new SparkConf()
      .setAppName("ClusteringJob")
      .set("spark.cassandra.connection.host", cassandraHost)
      .setMaster("local")

    val context = new SparkContext(conf)
    context.setLogLevel("ERROR")

    val job = new ClusteringJob(context)
    job.run(week, channel, modelPath)
    context.stop()
  }
}
