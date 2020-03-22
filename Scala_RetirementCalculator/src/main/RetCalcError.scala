
sealed abstract class RetCalcError(val message: String)

object RetCalcError {

  case class MoreExpensesThanIncome(income: Double, expenses: Double) extends RetCalcError(
    s"Expenses: $expenses >= $income. You will never be able to save enough to retire"
  )

  case class ReturnMonthOutOfBounds(month: Int, maximum: Int) extends RetCalcError(
    s"Cannot get the return for month $month. Accepted range 0 to $maximum"
  )

}
