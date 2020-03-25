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

  private def probabilityDensity(labelledFeaturesVector: Vector, broadcastVariableStatsVectorPair: (Vector, Vector)): Double = {



  }
}
