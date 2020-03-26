import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.{DataFrame, SparkSession}

trait FraudDetectionWrapper {

  val trainSetFileName = "training.csv"
  val testSetFileName = "testing.csv"

  lazy val session = SparkSession.builder()
    .appName("FraudDetection")
    .master("local")
    .getOrCreate()

  val dataSetPath = "/media/HardDrive/CODE/TRAINING/Scala_TrainingProjects/FraudDetection/"

  val fdTrainSet_EDA = ("summary","fdEdaFeaturesVectors")
  val fdFeatures_IndexedLabel_Train = ("fd-features-vectors","label")
  val fdFeatures_IndexedLabel_CV = ("fd-features-vectors","label")


  def buildTestVectors(trainPath: String): DataFrame = {

    def analyzeFeatureMeasurements: Array[(org.apache.spark.ml.linalg.Vector, String)] = {
      val featureVectors = session.sparkContext
        .textFile(trainPath, 2)
        .flatMap { featureLine => featureLine.split("\n").toList}
        .map(_.split(","))
        .collect
        .map((featureLine => (Vectors.dense(featureLine(0).toDouble, featureLine(1).toDouble), featureLine(2))))
      featureVectors
    }

    val fdDF = session.createDataFrame(analyzeFeatureMeasurements).toDF(fdFeatures_IndexedLabel_CV._1, fdFeatures_IndexedLabel_CV._2)
    fdDF.cache()
  }
}
