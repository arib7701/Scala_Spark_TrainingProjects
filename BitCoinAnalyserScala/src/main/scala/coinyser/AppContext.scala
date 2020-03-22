package coinyser

import java.net.URI

import cats.effect.{IO, Timer}
import org.apache.spark.sql.SparkSession

class AppContext(val transactionStorePath: URI)(implicit val spark: SparkSession, implicit val timer: Timer[IO]) {

}
