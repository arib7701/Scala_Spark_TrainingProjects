name := "StockPricePredictions"

version := "0.1"

scalaVersion := "2.11.12"

scalacOptions ++= List("-feature","-deprecation", "-unchecked", "-Xlint")

mainClass in (Compile, run) := Some("scala.StockPricePipeline")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.3.1",
  "org.apache.spark" %% "spark-mllib" % "2.3.1",
  "org.apache.spark" %% "spark-sql" % "2.3.1"

)

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
fork in run := true
fork in test := true