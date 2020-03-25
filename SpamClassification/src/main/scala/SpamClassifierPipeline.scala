import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification.NaiveBayes
import org.apache.spark.ml.feature._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

case class LabeledMailSpam(label: Double, mailSentence: String)

object SpamClassifierPipeline extends SpamWrapper {

  def main(args: Array[String]): Unit = {

    // Remove INFO log
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.spark-project").setLevel(Level.ERROR)

    val regex1 = raw"[^A-Za-z0-9\s]+" // keep only letters and numericals
    val regex2 = raw"[^A-Za-z\s]+" // keep only letters

    // Clean mail data input
    val mailRDD: RDD[String] = session.sparkContext.textFile(mailFileName)
    val mailNoPunctuationRDD: RDD[String] = mailRDD.map(_.replaceAll(regex1, "").trim.toLowerCase)

    val cleanedMailRDD: RDD[LabeledMailSpam] = mailNoPunctuationRDD
      .repartition(4)
      .map(m => LabeledMailSpam(0.0, m)) // 0 label for good mail

    // Clean spam data input
    val spamRDD: RDD[String] = session.sparkContext.textFile(spamFileName)
    val spamNoPunctuationRDD: RDD[String] = spamRDD.map(_.replaceAll(regex1, "").trim.toLowerCase)

    val cleanedSpamRDD: RDD[LabeledMailSpam] = spamNoPunctuationRDD
      .repartition(4)
      .map(s => LabeledMailSpam(1.0, s)) // 1 label for spam

    // Combine two RDD
    val mailAndSpam: RDD[LabeledMailSpam] = cleanedMailRDD ++ cleanedSpamRDD


    // Create DF with two columns
    val mailAndSpamDF: DataFrame = session.createDataFrame(mailAndSpam).toDF("label", "lowerCasedSentences")
    val sentenceDF: DataFrame = mailAndSpamDF.select(col("lowerCasedSentences"), col("label"))

    sentenceDF.printSchema()
    sentenceDF.columns

    // Drop any rows with null values
    val nonNullBagOfWordsDF: DataFrame = sentenceDF.na.drop(Array("lowerCasedSentences"))

    nonNullBagOfWordsDF.printSchema()
    nonNullBagOfWordsDF.columns

    // Tokenize sentences - split on white space
    val tokens = new Tokenizer()
      .setInputCol("lowerCasedSentences")
      .setOutputCol("mailFeatureWords")

    val tokenizedBagOfWordsDF: DataFrame = tokens.transform(nonNullBagOfWordsDF)
   // println("tokenizedBagOfWordsDB is : " + tokenizedBagOfWordsDF.show)


    // Remove Stop Words
    val stopWordsRemover = new StopWordsRemover()
      .setInputCol("mailFeatureWords")
      .setOutputCol("noStopWordsMailFeatures")

    val noStopWordsDF: DataFrame = stopWordsRemover.transform(tokenizedBagOfWordsDF)
    //println("noStopWordsDF is : " + noStopWordsDF.show)

    // Hash Bag of Words
    val hashMapper = new HashingTF()
      .setInputCol("noStopWordsMailFeatures")
      .setOutputCol("mailFeatureHashes")
      .setNumFeatures(10000)

    val featurizedDF = hashMapper.transform(noStopWordsDF)
   // println("featurizedDF is : " + featurizedDF.show)

    // Create train and test sets
    val splitFeaturizedDF: Array[DataFrame] = featurizedDF.randomSplit(Array(0.80, 0.20), 98765L)
    val trainFeaturizedDF: DataFrame = splitFeaturizedDF(0)
    val testFeaturizedDF: DataFrame = splitFeaturizedDF(1)

    println("TEST DATASET set count is: " + testFeaturizedDF.count())
    println("TRAIN DATASET set count is: " + trainFeaturizedDF.count())
    //println("trainFeaturizedDF looks like this: " + trainFeaturizedDF.show)

    // Remove unnecessary columns
    val cleanedTrainFeaturizedDF: DataFrame = trainFeaturizedDF.drop("mailFeatureWords", "noStopWordsMailFeatures", "mailFeaturesHashes")
    val cleanedTestFeaturizedDF: DataFrame = testFeaturizedDF.drop("mailFeatureWords", "noStopWordsMailFeatures", "mailFeaturesHashes")
    //println("cleanedTrainFeaturizedDF looks like this: " + cleanedTrainFeaturizedDF.show)


    // Apply IDF : Inverse Document Frequency
    val mailIDF = new IDF()
      .setInputCol("mailFeatureHashes")
      .setOutputCol("mailIDF")

    mailIDF.fit(featurizedDF)


    // Set Normalizer
    val normalizer = new Normalizer()
      .setInputCol("mailIDF")
      .setOutputCol("features")

    // Naive Bayes Algorithm

    val naiveBayes = new NaiveBayes()
      .setFeaturesCol("features")
      .setPredictionCol("prediction")

    // Create Pipeline with all previous stages

    val spamPipeline = new Pipeline()
      .setStages(Array[PipelineStage](tokens) ++
        Array[PipelineStage](stopWordsRemover) ++
        Array[PipelineStage](hashMapper) ++
        Array[PipelineStage](mailIDF) ++
        Array[PipelineStage](normalizer) ++
        Array[PipelineStage](naiveBayes))

    // Fit pipeline on train set
    val mailModel = spamPipeline.fit(cleanedTrainFeaturizedDF.drop("mailFeatureHashes"))

    // Make prediction on test set
    val rawPredictions: DataFrame = mailModel.transform(cleanedTestFeaturizedDF.drop("mailFeatureHashes"))
    println("Predictions are: " + rawPredictions.show)

    val predictions = rawPredictions.select(col("lowerCasedSentences"), col("prediction")).cache
    println("Displaying predictions : " + predictions.show(50))

    session.stop()

  }
}
