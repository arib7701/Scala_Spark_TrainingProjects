

object SimulatePlanApp extends App {

  println(strMain(args))

  def strMain(strings: Array[String]): String = {

    val (from +: until +: Nil) = strings(0).split(",")toList
    val nbOfYearsSaving = strings(1).toInt
    val nbOfYearsInRetirement = strings(2).toInt

    val allReturns = Returns.fromEquityAndInflationData(equities = EquityData.fromResource("sp500.tsv"), inflations = InflationData.fromResource("cpi.tsv"))

    val returns = allReturns.fromUntil(from, until)

    RetCalc.simulatePlan(
      returns = returns,
      params = RetCalcParams(nbOfMonthsInRetirement = nbOfYearsInRetirement * 12, netIncome = strings(3).toInt, currentExpenses = strings(4).toInt, initialCapital = strings(5).toInt),
      nbOfMonthsSaving = nbOfYearsSaving * 12
    ) match {

      case Right((capitalAtRetirement, capitalAfterDeath)) =>
        s"""
           |Capital after $nbOfYearsSaving years of savings: ${capitalAtRetirement.round}
           |Capital after $nbOfYearsInRetirement years in retirements: ${capitalAfterDeath.round}
        """.stripMargin

      case Left(err) => err.message
    }
  }
}
