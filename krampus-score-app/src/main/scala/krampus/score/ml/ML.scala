// Copyright (C) 2017, codejitsu.

package krampus.score.ml

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vector

object ML extends LazyLogging {
  def distance(a: Vector, b: Vector): Double =
    math.sqrt(a.toArray.zip(b.toArray).map(p => p._1 - p._2).map(d => d * d).sum)

  def probablyAnomaly(model: KMeansModel, counter: Int, epsilon: Option[Double], label: String): Boolean = {
    val centroids = model.clusterCenters

    val counterVector = Vectors.dense(counter)

    val predicted = centroids(model.predict(counterVector))
    val dist = distance(predicted, counterVector)

    val eps = epsilon.getOrElse(0.1)

    if (predicted(0) * eps < dist) {
      logger.info(s"Anomaly detected $label: counter = $counter, predicted = ${predicted(0)}, dist = $dist, " +
        s"eps = $eps, distance > predicted * eps? TRUE")

      true
    } else {
      logger.info(s"NO anomaly detected $label: counter = $counter, predicted = ${predicted(0)}, dist = $dist, " +
        s"eps = $eps, distance > predicted * eps? FALSE")

      false
    }
  }
}
