package lr

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row}

object BreastCancerLrPipeline extends BreastCancerWrapper {

  def main(args: Array[String]): Unit = {

    // Remove INFO log
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.spark-project").setLevel(Level.ERROR)

    // create dataframe
    val dataSet: DataFrame = buildDataFrame(dataSetPath)

    // create indexer to map values to labels
    val indexer = new StringIndexer()
      .setInputCol(breast_cancer_columns._2)
      .setOutputCol(breast_cancer_columns._3)

    // fit model on dataset - set 2 as 0 and 4 as 1 for labels
    val indexerModel = indexer.fit(dataSet)
    val indexedDataFrame = indexerModel.transform(dataSet)
    indexedDataFrame.show


    val splitDataSet: Array[Dataset[Row]] = indexedDataFrame.randomSplit(Array(0.75, 0.25), 98765L)
    val trainDataSet = splitDataSet(0)
    val testDataSet = splitDataSet(1)

    println("Size of the new split dataset " + splitDataSet.size)
    println("TEST DATASET set count is: " + testDataSet.count())
    println("Training set count is: " + trainDataSet.count())

    /*
         - create logistic regression model
         - use elasticnet regularization to decrease effect of correlation between features
         - train model on trainSet
      */
    val logitModel = new LogisticRegression()
      .setElasticNetParam(0.75)
      .setFamily("auto")
      .setFeaturesCol(breast_cancer_columns._1)
      .setLabelCol(breast_cancer_columns._3)
      .fit(trainDataSet)

    // Run model on testSet
    val testDataPredictions = logitModel.transform(testDataSet)

    testDataPredictions.show(25)
    println("Coefficient matrix: " + logitModel.coefficientMatrix)
    println("Summary: " + logitModel.summary.predictions)

    // create pipeline with two stages: indexer and classifier
    val pipeline = new Pipeline()
      .setStages(Array[PipelineStage](indexer) ++ Array[PipelineStage](logitModel))

    // Train pipeline on trainSet
    val pipelineModel = pipeline.fit(trainDataSet.drop("label"))

    // Make predictions on testSet
    val predictions = pipelineModel.transform(testDataSet.drop("label"))

    // Evaluate Predictions
    val modelOutputAccuracy: Double = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setMetricName("areaUnderROC")
      .setRawPredictionCol("prediction")
      .setRawPredictionCol("rawPrediction")
      .evaluate(testDataPredictions)

    println("Accuracy of predictions " + modelOutputAccuracy)

    val predictionAndLabels: DataFrame = predictions.select("prediction", "label")

    val validateRDD2: RDD[(Double, Double)] = predictionAndLabels
      .rdd
      .collect {
        case Row(predictionValue: Double, labelValue: Double) => (predictionValue, labelValue)
      }

    val classifierMetrics = new BinaryClassificationMetrics(validateRDD2)
    val accuracyMetrics = (classifierMetrics.areaUnderROC(), classifierMetrics.areaUnderPR())

    println(s"Area under Receiver Operating Characteristic (ROC) Curse is ${accuracyMetrics._1}")
    println(s"Area under Precision Recall (PR) Curse is ${accuracyMetrics._2}")
  }

}
