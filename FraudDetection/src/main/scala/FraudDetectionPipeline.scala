import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.DataFrame

object FraudDetectionPipeline extends FraudDetectionWrapper {

  def main(args: Array[String]): Unit = {

    // Remove INFO log
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.spark-project").setLevel(Level.ERROR)


    // Set Up train set
    val trainSet: DataFrame = session.read
      .format("csv")
      .option("inferSchema", "true")
      .load(dataSetPath + trainSetFileName)

    // cache trainSet
    val trainSetCached = trainSet.cache()
    println("Cached Training DataFrame looks like below: ")
    trainSetCached.show()

    val trainSetEdaStats: DataFrame = trainSetCached.summary()
    println("Training DataFrame Summary looks like below: ")
    trainSetEdaStats.show()


    // Set up test set
    val testSetCached : DataFrame = buildTestVectors(dataSetPath + testSetFileName)
    println("Testing DataFrame looks like below: ")
    testSetCached.show()

    // Get Mean of train set stats
    val trainSetMeanDF: DataFrame = trainSetEdaStats.where("summary == 'mean'")
    print("Means dataset is: ")
    trainSetMeanDF.show()

    // Convert to array of rows with mean values
    val meanPairs : Array[(String, String)] = trainSetMeanDF
      .collect()
      .map(row => (row.getString(1), row.getString(2)))

    val transactionMean = meanPairs(0)._1.toDouble
    val distanceMean = meanPairs(0)._2.toDouble

    // Get StdDev of train set stats
    val trainSetStdDevDF: DataFrame = trainSetEdaStats.where("summary == 'stddev'")
    print("StdDev dataset is: ")
    trainSetStdDevDF.show()

    // Convert to array of rwos with stddev values
    val stddevPairs: Array[(String, String)] = trainSetStdDevDF
      .collect()
      .map(row => (row.getString(1), row.getString(2)))

    val transationStdDev = stddevPairs(0)._1.toDouble
    val distanceStdDev = stddevPairs(0)._2.toDouble

    // Build tuple for mean and stddev
    val meanStdDevTuples = ((transactionMean, distanceMean), (transationStdDev, distanceStdDev))
    println("meanStdDevTuples is : " + meanStdDevTuples)

    // Wrap tuple in DenseVector for broadcasting
    val meansVector = new DenseVector(Array(meanStdDevTuples._1._1, meanStdDevTuples._1._2))
    println("Transaction Mean and Distance Mean Vector looks like this: " + meansVector.toArray.mkString(" "))

    val stdDevVector = new DenseVector(Array(meanStdDevTuples._2._1, meanStdDevTuples._2._2))
    println("Distance Mean and Distance SD Vector looks like this: " + stdDevVector.toArray.mkString(" "))

    // Create instance of Fraud Detection Algorithm
    val outlierAlgorithm = new OutlierAlgorithm(testSetCached, meansVector, stdDevVector)
    outlierAlgorithm.tuneModel()

  }
}
