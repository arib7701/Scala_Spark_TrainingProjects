
sealed trait Returns

object Returns {

  def monthlyRate(returns: Returns, month: Int): Either[RetCalcError, Double] = {
    returns match {
      case FixedReturns(r) => Right(r / 12)
      case VariableReturns(rs) => {
        if (rs.isDefinedAt(month))
          Right(rs(month).monthlyRate) // get rs(index in vector).monthlyRate
        else
          Left(RetCalcError.ReturnMonthOutOfBounds(month, rs.size - 1))
      }
      case OffsetReturns(rs, offset) => {
        monthlyRate(rs, month + offset)
      }
    }
  }

  def fromEquityAndInflationData(equities: Vector[EquityData], inflations: Vector[InflationData]) : VariableReturns = {

    /*
     zip create a collection of tuples (EquityData, InflationData) - tuples collection will have smallest size of the two
     sliding(p) create an Iterator which produce a collection of size p - it will have elem p and all elem of p-1
     collect (like map) transform elem of collection but can as well filter them using pattern matching
     here: filter out sliding elem of size 0 or 1
    */
    VariableReturns(equities.zip(inflations).sliding(2).collect {

      case (prevEquity, prevInflation) +: (equity, inflation) +: Vector() =>
        val inflationRate = inflation.value / prevInflation.value
        val totalReturn = (equity.value + equity.monthlyDividend) / prevEquity.value
        val realTotalReturn = totalReturn - inflationRate
        VariableReturn(equity.monthId, realTotalReturn)
    }.toVector)
  }
}

case class FixedReturns(annualRate: Double) extends Returns

case class VariableReturns(returns: Vector[VariableReturn]) extends Returns {

  /*
    dropWhile HOG drop elements until condition is reached
    takeWhile HOG keep elements until condition is reached
    here: remove all elem before monthIdFrom, keep elem untim monthIdUntil, discard the one after monthIdUntil
   */
  def fromUntil(monthIdFrom: String, monthIdUntil: String) : VariableReturns =
    VariableReturns(
      returns
        .dropWhile(_.monthId != monthIdFrom)
        .takeWhile(_.monthId != monthIdUntil))

}

case class VariableReturn(monthId: String, monthlyRate: Double)

case class OffsetReturns(orig: Returns, offset: Int) extends Returns