
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.{DataFrame, SparkSession}

trait IrisWrapper {

  lazy val session: SparkSession = SparkSession
    .builder()
    .appName("Iris Pipeline")
    .master("local")
    .getOrCreate()

  val datasetPath = "/media/HardDrive/CODE/TRAINING/Scala_TrainingProjects/Classify_Flower/iris.data"

  val irisColumns = ("iris-features-column", "iris-species-column", "label")

  def buildDataFrame(dataSet: String): DataFrame = {

    /*
        - read textFile
        - split each row using \n separator
        - for each row, split at each ,
        - drop header row
        - map Array[Array[String]] to Array[(Vector, String)]

        - create dataframe using getRows fct
     */

    def getRows: Array[(org.apache.spark.ml.linalg.Vector, String)] = {
      session.sparkContext
        .textFile(dataSet)
        .flatMap { partitionLine => partitionLine.split("\n").toList }
        .map(_.split(","))
        .collect()
        .map(row => (Vectors.dense(row(0).toDouble, row(1).toDouble, row(2).toDouble, row(3).toDouble), row(4)))
    }

    session.createDataFrame(getRows).toDF(irisColumns._1, irisColumns._2)
  }
}