package bugapp

import java.time.OffsetDateTime
import java.time.temporal.IsoFields

import org.scalatest.FunSuite

/**
  *
  */
class DateTests extends FunSuite {
  test("w") {
    val date = OffsetDateTime.now()
    println(date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
  }
}
