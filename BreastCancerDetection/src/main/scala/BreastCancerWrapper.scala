import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.{DataFrame, SparkSession}

trait BreastCancerWrapper {

  lazy val session: SparkSession = SparkSession
    .builder()
    .appName("Breast Cancer Detection")
    .master("local")
    .getOrCreate()

  val dataSetPath = "/media/HardDrive/CODE/TRAINING/Scala_TrainingProjects/BreastCancerDetection/breast-cancer-wisconsin.data"

  val breast_cancer_columns = ("features", "bwc-diagnoses-column", "label")

  def buildDataFrame(dataSet: String): DataFrame = {

    def getRows: Array[(org.apache.spark.ml.linalg.Vector, String)] = {
      session.sparkContext
        .textFile(dataSet)
        .flatMap { partition => partition.split("\n").toList }
        .map(_.split(","))
        .filter(_ (5) != "?")
        .collect
        .map(row => (Vectors.dense(row(0).toDouble, row(1).toDouble, row(2).toDouble, row(3).toDouble,
          row(4).toDouble, row(5).toDouble, row(6).toDouble, row(7).toDouble, row(8).toDouble), row(9)))
    }

    session.createDataFrame(getRows).toDF(breast_cancer_columns._1, breast_cancer_columns._2)
  }
}
