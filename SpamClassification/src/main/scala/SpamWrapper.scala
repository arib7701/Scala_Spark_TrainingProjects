
import org.apache.spark.sql.SparkSession

trait SpamWrapper {

  val mailFileName = "inbox.txt"
  val spamFileName = "junk.txt"

  lazy val session = SparkSession.builder()
    .appName("SpamClassification")
    .master("local")
    .getOrCreate()
}
