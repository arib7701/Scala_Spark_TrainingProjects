import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{DateType, DoubleType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}


case class Stock(dt: String, openPrice: Double, highPrice: Double, lowPrice: Double, closePrice: Double, volume: Double, adjClosePrice: Double)

trait StockWrapper {

  lazy val session: SparkSession = {
    SparkSession
      .builder()
      .master("local")
      .appName("Stock-Price-Pipeline")
      .getOrCreate()
  }

  val dataSetPath = "/media/HardDrive/CODE/TRAINING/Scala_TrainingProjects/StockPricePredictions/News.csv";

  def buildStockFrame(dataFile: String): DataFrame = {
    def getRows2: Array[(org.apache.spark.ml.linalg.Vector, String)] = {
      session.sparkContext.textFile(dataFile).flatMap {
        partitionLine => partitionLine.split("\n").toList
      }.map(_.split(",")).collect.drop(1).map(row => (Vectors.dense(row(1).toDouble, row(2).toDouble, row(3).toDouble, row(4).toDouble, row(5).toDouble), row(6)
      )
      )
    }

    val dataFrame = session.createDataFrame(getRows2).toDF
    val stockFrameCached = dataFrame.cache

    stockFrameCached
  }

  val stockSchema: StructType = StructType(Array(
    StructField("Date", DateType, false),
    StructField("Open", DoubleType, false),
    StructField("High", DoubleType, true),
    StructField("low", DoubleType, true),
    StructField("Close", DoubleType, true),
    StructField("Adj_Close", DoubleType, true),
    StructField("Volume", DoubleType, true)
  ))


  def buildStockFrame2(dataFile: String): DataFrame = {
    session.read
      .format("csv")
      .option("header", "true")
      .schema(stockSchema)
      .option("nullValue", "")
      .option("treatEmptyValuesAsNulls", "true")
      .load(dataFile)
      .cache()
  }

  def parseStock(str: String) : Stock = {
    val line = str.split(",")
    Stock(line(0), line(1).toDouble, line(2).toDouble, line(3).toDouble, line(4).toDouble, line(5).toDouble, line(6).toDouble)
  }

  def parseRDD(rdd: RDD[String]): RDD[Stock] = {
    val header = rdd.first
    rdd.filter(_(0) != header(0))
      .map(parseStock)
      .cache()
  }
}
