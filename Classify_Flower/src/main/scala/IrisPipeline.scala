import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row}

object IrisPipeline extends IrisWrapper {

  def main(args: Array[String]): Unit = {

    val dataSet = buildDataFrame(datasetPath)

    // split data in test and train datasets
    val splitDataSet: Array[Dataset[Row]] = dataSet.randomSplit(Array(0.85, 0.15), 98765L)

    val trainDataSet = splitDataSet(0)
    val testDataSet = splitDataSet(1)

    println("Size of the new split dataset " + splitDataSet.size)
    println("TEST DATASET set count is: " + testDataSet.count())
    println("Training set count is: " + trainDataSet.count())


    //-----------------------------------------------------------------------------------------------------------

    // build indexer to map label column to indexed learned column
    val indexer = new StringIndexer()
      .setInputCol(irisColumns._2)
      .setOutputCol(irisColumns._3)

    // build random forest classifier algo
    val randomForestClassifier = new RandomForestClassifier()
      .setFeaturesCol(irisColumns._1)
      .setFeatureSubsetStrategy("sqrt")

    // build pipeline = indexer + randomforest classifier
    val irisPipeline = new Pipeline()
      .setStages(Array[PipelineStage](indexer) ++ Array[PipelineStage](randomForestClassifier))

    // set hyperparameters

    val rfWithParam = randomForestClassifier
      .setNumTrees(15)
      .setMaxDepth(2)
      .setImpurity("gini")

    val gridBuilder = new ParamGridBuilder()
      .addGrid(rfWithParam.numTrees, Array(8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96))
      .addGrid(rfWithParam.maxDepth, Array(4, 10, 16, 22, 28))
      .addGrid(rfWithParam.impurity, Array("gini", "entropy"))
      .build()

    /* val rfMax_Depth = rfNum_Trees.setMaxDepth(2)
     val gridBuilder2 = gridBuilder1.addGrid(rfMax_Depth.maxDepth, Array(4,10,16,22,28))

     val rfImpurity = rfMax_Depth.setImpurity("gini")
     val gridBuilder3 = gridBuilder2.addGrid(rfImpurity.impurity, Array("gini", "entropy"))

     val finalParamGric: Array[ParamMap] = gridBuilder3.build()*/

    //-----------------------------------------------------------------------------------------------------------

    /*
         - split training set into validation and training set
         - set seed
         - pass hyper parameters
         - pass pipeline
         - set up evaluator as multi classification
         - set train ratio
         - train on train set to get model
         - use model on test set
     */

    val validatedTestResults: DataFrame = new TrainValidationSplit()
      .setSeed(1234567L)
      .setEstimatorParamMaps(gridBuilder)
      .setEstimator(irisPipeline)
      .setEvaluator(new MulticlassClassificationEvaluator())
      .setTrainRatio(0.85)
      .fit(trainDataSet)
      .transform(testDataSet)

    println(validatedTestResults.show(100))

    val validatedTestResultsDataset: DataFrame = validatedTestResults.select("prediction", "label")
    println(validatedTestResultsDataset.take(10))

    val validatedRDD : RDD[(Double, Double)] = validatedTestResultsDataset
      .rdd
      .collect {
        case Row(predictionValue: Double, labelValue: Double) => (predictionValue, labelValue)
      }

    //-----------------------------------------------------------------------------------------------------------


    val modelOutputAccuracy: Double = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setMetricName("accuracy")
      .setPredictionCol("prediction")
      .evaluate(validatedTestResultsDataset)

    println(s"Model output accuracy $modelOutputAccuracy")


    val multiClassMetrics = new MulticlassMetrics(validatedRDD)
    val accuracyMetrics = (multiClassMetrics.accuracy, multiClassMetrics.weightedPrecision)
    val accuracy = accuracyMetrics._1
    val weightedPrecision = accuracyMetrics._2

    println(s"Accuracy $accuracy and weighted precision $weightedPrecision")


  }
}
