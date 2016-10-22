package bugapp

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.{IsoFields, WeekFields}

import org.scalatest.FunSuite

/**
  *
  */
class DateTests extends FunSuite {
  test("Week of Year for date 01/APR/2016 should be 13") {
    val date = OffsetDateTime.of(2016, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    assert(week == 13)
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

  test("Build period") {
    val to = OffsetDateTime.of(2016, 10, 19, 23, 0, 0, 0, ZoneOffset.UTC)
    val weeks = 4
    val field = WeekFields.ISO.dayOfWeek()

    (1 to 10 by 1).map { week =>
      val from = to.minusWeeks(week).`with`(field, 1)
      println(s"${DateTimeFormatter.ISO_LOCAL_DATE.format(from)}")
      from
    }
  }
}
