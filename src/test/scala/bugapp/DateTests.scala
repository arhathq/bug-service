package bugapp

import java.time._
import java.time.temporal.ChronoField

import org.scalatest.FunSuite

/**
  *
  */
class DateTests extends FunSuite {
  test("Week of Year for date 01/FEB/2016 should be 5") {
    val date = OffsetDateTime.of(2016, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val week = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR)

    assert(week == 5)
  }

  test("Period in days between dates 01/OCT/2016 and 05/OCT/2016 should be 4") {
    val from = LocalDate.of(2016, 10, 1)
    val to = LocalDate.of(2016, 10, 5)
    val period = Period.between(from, to)

    assert(period.getDays == 4)
  }

  test("Duration in days from 01/OCT/2016 and to 05/OCT/2016 should be 4") {
    val from = OffsetDateTime.of(2016, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val to = OffsetDateTime.of(2016, 10, 5, 0, 0, 0, 0, ZoneOffset.UTC)
    val duration = Duration.between(from, to)

    assert(duration.toDays == 4)
  }
}
