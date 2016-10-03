package bugapp

import java.time.OffsetDateTime
import java.time.temporal.{ChronoField}

import org.scalatest.FunSuite

/**
  *
  */
class DateTests extends FunSuite {
  test("w") {
    val date = OffsetDateTime.now()
    println(date.get(ChronoField.ALIGNED_WEEK_OF_YEAR))
  }
}
