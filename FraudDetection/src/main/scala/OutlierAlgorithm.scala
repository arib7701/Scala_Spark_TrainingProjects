import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.DataFrame

import scala.collection.immutable.ListMap

class OutlierAlgorithm(testingDF: DataFrame, meansVector: DenseVector, stdDevVector: DenseVector) extends Serializable with FraudDetectionWrapper {

  val scores: ListMap[Int, Double] = new ListMap[Int, Double]()

  def tuneModel(): ListMap[Int, Double] = {

    val broadcastVariable = session.sparkContext.broadcast((meansVector, stdDevVector))
    println("Broadcast Variable is: " + broadcastVariable)

    import session.implicits._

    val fdProbabilityDensities: DataFrame = testingDF.map(row => probabilityDensity(row.getAs(0), broadcastVariable.value)).toDF("PDF")

    // Persist data to default storage (MEMORY AND DISK)
    testingDF.persist()

    // Find best error
    val finalScores: ListMap[Int, Double] = evalScores(testingDF, fdProbabilityDensities)
    println("Final Scores are: " + finalScores.mkString(" "))

    finalScores
  }

  private def probabilityDensity(labelledFeaturesVector: DenseVector, broadcastVariableStatsVectorPair: (DenseVector, DenseVector)): Double = {

    def featureDoubles(features: Array[Double], transactionsSdMeanStats: Array[Double], distanceSdMeanStats: Array[Double]): List[(Double, Double, Double)] = {
      (features, transactionsSdMeanStats, distanceSdMeanStats).zipped.toList
    }

    val pDfCalculator: List[(Double, Double, Double)] = featureDoubles(
      labelledFeaturesVector.toArray,
      broadcastVariableStatsVectorPair._1.toArray,
      broadcastVariableStatsVectorPair._2.toArray
    )

    val probabilityDensityValue: Double = pDfCalculator.map(pDf => new NormalDistribution(pDf._2, pDf._3).density(pDf._1)).product

    probabilityDensityValue
  }

  def evalScores(testingDF: DataFrame, probabilityDensities: DataFrame): ListMap[Int, Double] = {

    val maxMinArray: Array[Double] = probabilityDensities
      .collect()
      .map(pbRow => pbRow.getDouble(0))

    println("Max of probabilities is: " + maxMinArray.max)
    println("Min of probabilities is: " + maxMinArray.min)

    var bestErrorTermValue = 0D
    var bestF1Measure = 0D

    val stepsize = (maxMinArray.max - maxMinArray.min) / 1000.0
    println("Step size is : " + stepsize)

    for (errorTerm <- maxMinArray.min to maxMinArray.max by stepsize) {
      val broadCastederrorTerm: Broadcast[Double] = session.sparkContext.broadcast(errorTerm)
      val broadcastTerm: Double = broadCastederrorTerm.value

      import session.implicits._
      val finalPreds: DataFrame = probabilityDensities.map { probRow =>
        if (probRow.getDouble(0) < broadcastTerm) {
          1.0
        } else 0.0
      }.toDF("PDF")

      val labelAndPredictions: DataFrame = testingDF.drop("label").crossJoin(finalPreds).cache()
      val fPs = positivesNegatives(labelAndPredictions, 0.0, 1.0)
      val tPs = positivesNegatives(labelAndPredictions, 1.0, 1.0)
      val fNs = positivesNegatives(labelAndPredictions, 1.0, 0.0)
      val precision = tPs / Math.max(1.0, tPs + fPs)
      val recall = tPs / Math.max(1.0, tPs + fNs)
      val f1Measure = 2.0 * precision * recall / (precision + recall)

      if (f1Measure > bestF1Measure) {
        bestF1Measure = f1Measure
        bestErrorTermValue = errorTerm
        scores + ((1, bestErrorTermValue), (2, bestF1Measure))
      }
    }

    scores
  }

  def positivesNegatives(labelAndPredictions: DataFrame, targetLabel: Double, finalPrediction: Double) : Double = {
    labelAndPredictions
      .filter(labelAndPrediction => labelAndPrediction.getAs("PDF") == targetLabel && labelAndPrediction.get(1) == finalPrediction)
      .count()
      .toDouble
  }
}
