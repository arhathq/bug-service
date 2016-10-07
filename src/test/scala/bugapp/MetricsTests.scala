package bugapp

import java.time.{OffsetDateTime, ZoneOffset}

import bugapp.bugzilla.Metrics
import bugapp.repository.{Bug, BugStats}
import org.scalatest.FunSuite

/**
  *
  */
class MetricsTests extends FunSuite {
//  val bugP1 = Bug(40061, "Critical", "P1", "CLOSED", "FIXED", "user@gmail.com", opened,
//    "user@gmail.com", changed, "Other", "Other", "", "Here is some critical summary", "", None)

  test("P1 bug that resolved in 2 days should pass SLA") {
    val opened = OffsetDateTime.of(2016, 9, 21, 11, 8, 0, 0, ZoneOffset.UTC)
    val changed = OffsetDateTime.of(2016, 9, 23, 12, 42, 14, 0, ZoneOffset.UTC)
    val priority = "P1"
    val (daysOpen, resolvedPeriod, passSla) = Metrics.age(priority, opened, Some(changed))
    assert(daysOpen == 2)
    assert(resolvedPeriod == Metrics.ResolvedIn2Days)
    assert(passSla)
  }

  test("P1 bug that resolved more than in 2 days should not pass SLA") {
    val opened = OffsetDateTime.of(2016, 9, 21, 11, 8, 0, 0, ZoneOffset.UTC)
    val changed = OffsetDateTime.of(2016, 9, 24, 11, 8, 0, 0, ZoneOffset.UTC)
    val priority = "P1"
    val (daysOpen, resolvedPeriod, passSla) = Metrics.age(priority, opened, Some(changed))
    assert(daysOpen == 3)
    assert(resolvedPeriod == Metrics.ResolvedIn6Days)
    assert(!passSla)
  }
}
