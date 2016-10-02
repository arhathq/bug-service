package bugapp.bugzilla

import java.time.{LocalDate, OffsetDateTime}

import bugapp.Implicits._

case class BugzillaBug(id: Int,
                       priority: String,
                       severity: String,
                       product: String,
                       component: String,
                       platform: String,
                       creator: String,
                       creation_time: OffsetDateTime,
                       status: String,
                       is_open: Boolean,
                       assigned_to: Option[String],
                       last_change_time: Option[OffsetDateTime],
                       summary: String,
                       cf_project_team: Option[String],
                       is_cc_accessible: Boolean,
                       url: String,
                       groups: Seq[String],
                       whiteboard: Option[String],
                       qa_contact: Option[String],
                       cf_v1_reference: Option[String],
                       estimated_time: Int,
                       remaining_time: Int,
                       resolution: Option[String],
                       classification: Option[String],
                       cf_versiononestate: Option[String],
                       cf_databasestoupdate: Option[String],
                       cf_hotdeploy_approved: Option[String],
                       flags: Seq[String],
                       cf_production: Option[String],
                       cf_customer_perspective: Option[String],
                       version: String,
                       cf_target_milestone: Option[String],
                       deadline: Option[String],
                       actual_time: Int,
                       is_creator_accessible: Boolean,
                       is_confirmed: Boolean,
                       target_milestone: Option[String]
                      )

case class BugzillaHistory(id: Int, alias: Option[String], historyItems: List[BugzillaHistoryItem])
case class BugzillaHistoryItem(when: OffsetDateTime, who: String, changes: List[BugzillaHistoryChange])
case class BugzillaHistoryChange(removed: String, added: String, field_name: String)
case class BugzillaParams(Bugzilla_login: String,
                          Bugzilla_password: String,
                          creation_time: Option[LocalDate] = None,
                          ids: Option[Seq[Int]] = None,
                          cf_production: Option[String] = Some("Production")) {
  import io.circe.syntax._

  def toJsonString: String = List(this).asJson.pretty(implicitly)
}
object BugzillaParams {
  def apply(username: String, password: String, startDate: LocalDate) = new BugzillaParams(username, password, creation_time = Some(startDate))
  def apply(username: String, password: String, ids: Seq[Int]) = new BugzillaParams(username, password, ids = Some(ids))
}

case class BugzillaRequest(method: String, params: BugzillaParams)
case class BugzillaResponse(error: Option[BugzillaError], id: String, result: Option[BugzillaResult])
case class BugzillaError(message: String, code: Int)
case class BugzillaResult(bugs: List[BugzillaBug])