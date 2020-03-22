package coinyser

import java.net.URI
import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect.{IO, Timer}
import cats.implicits._
import org.apache.spark.sql.functions.{explode, from_json, lit}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, Dataset, SaveMode, SparkSession}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object BatchProducer {

  val WaitTime: FiniteDuration = 59.minute
  val ApiLag: FiniteDuration = 5.seconds

  //---------------------------------------------------------------------------------------------------------------

  //write parquet format to file : SideEffect => unsafe
  def unsafeSave(transactions: Dataset[Transaction], path: URI): Unit =
    transactions
      .write // create a DataFrameWriter
      .mode(SaveMode.Append) // append content to end of file
      .partitionBy("date") // partition by date to optimize storage
      .parquet(path.toString) // final action method

  // Use IO from cats, will execute the code later when unsafeRunSync() is called
  def save(transactions: Dataset[Transaction], path: URI): IO[Unit] =
    IO(unsafeSave(transactions, path))

  //---------------------------------------------------------------------------------------------------------------

  // only one instance of spark session in app - so pass it as implicit
  def jsonToHttpTransaction(json: String)(implicit spark: SparkSession): Dataset[HttpTransaction] = {

    import spark.implicits._

    // toDS convert string to one row dataset
    val ds: Dataset[String] = Seq(json).toDS()

    // Get schema from empty Dataset
    val txSchema: StructType = Seq.empty[HttpTransaction].toDS().schema

    // Wrap in ArrayType
    val schema = ArrayType(txSchema)

    // Parse Json Column using schema
    val arrayColumn: Column = from_json($"value", schema)

    // Use explode (like flatten vector) to get one row per element of the array
    // Use select to get all the rows
    // Convert as a DataFrame with date/tid/price columns
    ds.select(explode(arrayColumn).alias("v"))
      .select("v.*")
      .as[HttpTransaction]
  }

  //---------------------------------------------------------------------------------------------------------------

  def httpToDomainTransactions(ds: Dataset[HttpTransaction]): Dataset[Transaction] = {

    import ds.sparkSession.implicits._

    ds.select(
      $"date".cast(LongType).cast(TimestampType).as("timestamp"),
      $"date".cast(LongType).cast(TimestampType).cast(DateType).as("date"),
      $"tid".cast(IntegerType),
      $"price".cast(DoubleType),
      $"type".cast(BooleanType).as("sell"),
      $"amount".cast(DoubleType)).as[Transaction]
  }

  //---------------------------------------------------------------------------------------------------------------

  def processOneBatch(fetchNextTransactions: IO[Dataset[Transaction]], transactions: Dataset[Transaction],
                      saveStart: Instant, saveEnd: Instant)
                     (implicit appCxt: AppContext): IO[(Dataset[Transaction], Instant, Instant)] = {

    val transactionsToSave = filterTxs(transactions, saveStart, saveEnd)
    for {
      _ <- BatchProducer.save(transactionsToSave, appCxt.transactionStorePath)
      _ <- IO.sleep(WaitTime)

      beforeRead <- currentInstant
      end = beforeRead.minusSeconds(ApiLag.toSeconds)
      nextTransactions <- fetchNextTransactions
    } yield (nextTransactions, saveEnd, end)

  }

  def filterTxs(transactions: Dataset[Transaction], fromInstant: Instant, untilInstant: Instant): Dataset[Transaction] = {

    import transactions.sparkSession.implicits._

    transactions.filter(
      ($"timestamp" >= lit(fromInstant.getEpochSecond).cast(TimestampType))
        &&
        ($"timestamp" < lit(untilInstant.getEpochSecond).cast(TimestampType)))
  }

  def currentInstant(implicit timer: Timer[IO]): IO[Instant] =
    timer.clockRealTime(TimeUnit.SECONDS) map Instant.ofEpochSecond


  //---------------------------------------------------------------------------------------------------------------

  def processRepeatedly(initialJsonTxs: IO[Dataset[Transaction]], jsonTxs: IO[Dataset[Transaction]])(implicit appContext: AppContext): IO[Unit] = {

    for {
      beforeRead <- currentInstant
      firstEnd = beforeRead.minusSeconds(ApiLag.toSeconds)

      firstTxs <- initialJsonTxs
      firstStart = truncateInstant(firstEnd, 1.day)

      _ <- Monad[IO].tailRecM((firstTxs, firstStart, firstEnd)) {
        case(txs, start, instant) => processOneBatch(jsonTxs, txs, start, instant).map(_.asLeft)
      }
    } yield ()
  }

  def truncateInstant(instant: Instant, interval: FiniteDuration): Instant = {
    Instant.ofEpochSecond(instant.getEpochSecond / interval.toSeconds * interval.toSeconds)
  }
}
