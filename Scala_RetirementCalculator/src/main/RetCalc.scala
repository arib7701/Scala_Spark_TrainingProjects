import RetCalcError.MoreExpensesThanIncome

import scala.annotation.tailrec

case class RetCalcParams(nbOfMonthsInRetirement: Int, netIncome: Int, currentExpenses: Int, initialCapital: Double)

object RetCalc {

  def nbOfMonthsSaving(returns: Returns, params: RetCalcParams) : Either[RetCalcError, Int] = {

    @tailrec
    def loop(months: Int) : Either[RetCalcError, Int] = {
      simulatePlan(returns = returns, nbOfMonthsSaving = months, params) match {
        case Left(err) => Left(err)
        case Right((capitalAtRetirement, capitalAfterDeath)) =>
          if (capitalAfterDeath > 0.0) Right(months)
          else loop(months + 1)
      }
    }

    if(params.netIncome > params.currentExpenses)
      loop(0)
    else
      Left(MoreExpensesThanIncome(params.netIncome, params.currentExpenses))  // to avoid infinite loop
  }


  def simulatePlan(returns: Returns, nbOfMonthsSaving: Int, params: RetCalcParams, monthOffset: Int = 0): Either[RetCalcError, (Double, Double)] = {

    for {
      capitalAtRetirement <- futureCapital(
        returns = OffsetReturns(returns, monthOffset),
        nbOfMonths = nbOfMonthsSaving, netIncome = params.netIncome, currentExpenses = params.currentExpenses, initialCapital = params.initialCapital)

      capitalAfterDeath <- futureCapital(
        returns = OffsetReturns(returns, monthOffset + nbOfMonthsSaving),
        nbOfMonths = params.nbOfMonthsInRetirement, netIncome = 0, currentExpenses = params.currentExpenses, initialCapital = capitalAtRetirement)
    } yield (capitalAtRetirement, capitalAfterDeath)

  }


  def futureCapital(returns: Returns, nbOfMonths: Int, netIncome: Int, currentExpenses: Int, initialCapital: Double): Either[RetCalcError, Double] = {

    val monthlySavings = netIncome - currentExpenses

    /* Replace by lambda anonymous fct

      def nextCapital(accumulated: Double, month: Int) : Double =
      accumulated * (1 + interestRate) + monthlySavings
    */

    // foldLeft HOG iterates through all months (1 to nb) apply fct
    (0 until nbOfMonths).foldLeft[Either[RetCalcError, Double]](Right(initialCapital)) {
      case (accumulated, month) => {
        // use for comprehension, if any call to futureCapital returns an error, the error will be return
        for {
          acc <- accumulated
          monthlyRate <- Returns.monthlyRate(returns, month)
        } yield acc * (1 + monthlyRate) + monthlySavings
      }
    }
  }
}
