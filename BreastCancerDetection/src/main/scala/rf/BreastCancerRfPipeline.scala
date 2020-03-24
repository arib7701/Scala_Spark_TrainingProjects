package rf

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row}

object BreastCancerRfPipeline extends BreastCancerWrapper {

  def main(args: Array[String]): Unit = {

    // Remove INFO log
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.spark-project").setLevel(Level.ERROR)


    // create train and test data sets
    val dataSet: DataFrame = buildDataFrame(dataSetPath)

    val splitDataSet: Array[Dataset[Row]] = dataSet.randomSplit(Array(0.85, 0.15), 98765L)
    val trainDataSet = splitDataSet(0)
    val testDataSet = splitDataSet(1)

    println("Size of the new split dataset " + splitDataSet.size)
    println("TEST DATASET set count is: " + testDataSet.count())
    println("Training set count is: " + trainDataSet.count())

    // create indexer to map values to labels
    val indexer = new StringIndexer()
      .setInputCol(breast_cancer_columns._2)
      .setOutputCol(breast_cancer_columns._3)

    // create random forest classifier
    val randomForestClassifier = new RandomForestClassifier()
      .setFeaturesCol(breast_cancer_columns._1)
      .setFeatureSubsetStrategy("sqrt")

    // create pipeline
    val pipeline = new Pipeline()
      .setStages(Array[PipelineStage](indexer) ++ Array[PipelineStage](randomForestClassifier))

    // set hyperparameters
    val rfWithHyperparameters = randomForestClassifier.setNumTrees(15)
      .setMaxDepth(2)
      .setImpurity("gini")

    val grid = new ParamGridBuilder()
      .addGrid(rfWithHyperparameters.numTrees, Array(8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96))
      .addGrid(rfWithHyperparameters.maxDepth, Array(4, 10, 16, 22, 28))
      .addGrid(rfWithHyperparameters.impurity, Array("gini", "entropy"))
      .build()

    // split into train and validation set
    // train model
    // use model on test data

    val validatedTestResults: DataFrame = new TrainValidationSplit()
      .setSeed(1234567L)
      .setEstimatorParamMaps(grid)
      .setEstimator(pipeline)
      .setEvaluator(new MulticlassClassificationEvaluator())
      .setTrainRatio(0.8)
      .fit(trainDataSet)
      .transform(testDataSet)

    println("Validated dataset is: " + validatedTestResults.show(100))

    // Review results

    val validatedTestResultDataset: DataFrame = validatedTestResults.select("prediction", "label")
    println("Validated TestSet Results Dataset is " + validatedTestResultDataset.take(10))

    val validatedRDD2: RDD[(Double, Double)] = validatedTestResultDataset
      .rdd
      .collect {
        case Row(predictionValue: Double, labelValue: Double) => (predictionValue, labelValue)
      }

    // Evaluate Model

    val modelOutputAccuracy: Double = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setMetricName("accuracy")
      .setPredictionCol("prediction")
      .evaluate(validatedTestResultDataset)

    println("Accuracy of Model Output results on the dataset: " + modelOutputAccuracy)

    val multiClassMetrics = new MulticlassMetrics(validatedRDD2)
    val accuracyMetrics = (multiClassMetrics.accuracy, multiClassMetrics.weightedPrecision)
    println("Accuracy is " + accuracyMetrics._1 + " - weighted precision is " + accuracyMetrics._2)
  }
}
