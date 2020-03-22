
import org.scalactic.{Equality, TolerantNumerics, TypeCheckedTripleEquals}
import org.scalatest.{EitherValues, Matchers, WordSpec}

class RetCalcSpec extends WordSpec with Matchers with TypeCheckedTripleEquals with EitherValues {

  // tolerance for comparing double value
  implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.0001)

  val params = RetCalcParams(
    nbOfMonthsInRetirement = 40 * 12,
    netIncome = 3000,
    currentExpenses = 2000,
    initialCapital = 10000)

  "RetCalc.futureCapital" should {
    "calculate how much savings will be left after having taken a pension for n months" in {
      val actual = RetCalc.futureCapital(returns = FixedReturns(0.04), nbOfMonths = 40 * 12, netIncome = 0,
        currentExpenses = 2000, initialCapital = 541267.1990).right.value
      val expected = 309867.53176
      actual should ===(expected)
    }

    "calculate the amount of savings I will have in n months" in {
      val actual = RetCalc.futureCapital(returns = FixedReturns(0.04), nbOfMonths = 25 * 12,
        netIncome = 3000, currentExpenses = 2000, initialCapital = 10000).right.value
      val expected = 541267.1990
      actual should ===(expected)
    }
  }

  "RetCalc.nbOfMonthsSaving" should {
    "calculate how long I need to save before I can retire" in {
      val actual = RetCalc.nbOfMonthsSaving(returns = FixedReturns(0.04), params = params).right.value
      val expected = 23 * 12 + 1
      actual should ===(expected)
    }

    "not crash if the resulting nbOfMonths is very high" in {
      val actual = RetCalc.nbOfMonthsSaving(returns = FixedReturns(0.01), params = RetCalcParams(
        nbOfMonthsInRetirement = 40 * 12,
        netIncome = 3000, currentExpenses = 2999, initialCapital = 0)).right.value
      val expected = 8280
      actual should ===(expected)
    }

    "not loop forever if I enter bad parameters" in {
      val actual = RetCalc.nbOfMonthsSaving(returns = FixedReturns(0.04), params.copy(netIncome = 1000)).left.value
      actual should ===(RetCalcError.MoreExpensesThanIncome(1000, 2000))
    }
  }

  "RetCalc.simulatePlan" should {
    "calculate the capital at retirement and the capital after death" in {
      val (capitalAtRetirement, capitalAfterDeath) = RetCalc.simulatePlan(returns = FixedReturns(0.04), nbOfMonthsSaving = 25 * 12, params).right.value
      capitalAtRetirement should ===(541267.1990)
      capitalAfterDeath should ===(309867.5316)
    }

    "use different returns for capitalisation and drawdown" in {
      val nbOfMonthsSaving = 25 * 12
      val returns = VariableReturns(Vector.tabulate(nbOfMonthsSaving + params.nbOfMonthsInRetirement)(i =>
        if (i < nbOfMonthsSaving)
          VariableReturn(i.toString, 0.04 / 12)
        else
          VariableReturn(i.toString, 0.03 / 12)))

      val (capitalAtRetirement, capitalAfterDeath) = RetCalc.simulatePlan(returns, nbOfMonthsSaving, params).right.value
      capitalAtRetirement should ===(541267.1990)
      capitalAfterDeath should ===(-57737.7227)
    }
  }
}
