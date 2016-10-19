package bugapp

import java.time.{OffsetDateTime, ZoneOffset}

import bugapp.bugzilla.Metrics
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

  test("Duration from 2016-10-10 to 2016-10-17 lasts in 5 business days") {
    val opened = OffsetDateTime.of(2016, 10, 10, 0, 0, 0, 0, ZoneOffset.UTC)
    val changed = OffsetDateTime.of(2016, 10, 17, 0, 0, 0, 0, ZoneOffset.UTC)
    val days = Metrics.durationInBusinessDays(opened, Some(changed))
    assert(days == 5)
  }

  test("Duration from 2016-09-23 to 2016-10-15 lasts in 16 business days") {
    val opened = OffsetDateTime.of(2016, 9, 23, 0, 0, 0, 0, ZoneOffset.UTC)
    val changed = OffsetDateTime.of(2016, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC)
    val days = Metrics.durationInBusinessDays(opened, Some(changed))
    assert(days == 16)
  }

  test("Marks weeks for September") {
    val startDate = OffsetDateTime.of(2016, 9, 30, 0, 0, 0, 0, ZoneOffset.UTC)
    val marks = Metrics.marks(startDate, 4)
    assert(marks.contains("2016-37"))
    assert(marks.contains("2016-38"))
    assert(marks.contains("2016-39"))
    assert(marks.contains("2016-40"))
  }

  test("Bugs splitting") {
    val bugs = List(B(1, "P2", ""), B(2, "P2", ""), B(3, "P1", ""),
      B(4, "P3", ""), B(5, "P1", ""), B(6, "P2", ""),
      B(7, "P3", ""), B(8, "P2", ""), B(9, "P3", ""))
    val (p1, other1) = bugs.partition(_.priority == "P1")
    val (p2, other2) = other1.partition(_.priority == "P2")
    val (p3, other3) = other2.partition(_.priority == "P3")
    println(p1)
    println(p2)
    println(p3)
  }

  case class B(id: Int, priority: String, sla: String)
}
