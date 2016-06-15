package bugapp.bugzilla

import io.circe.Encoder
import io.circe.generic.semiauto._

case class BugzillaBug(id: Int,
                       priority: String,
                       creator: String,
                       assigned_to: Option[String],
                       severity: String,
                       last_change_time: Option[String],
                       cf_project_team: Option[String],
                       is_cc_accessible: Boolean,
                       url: String,
                       groups: Seq[String],
                       creation_time: String,
                       whiteboard: Option[String],
                       qa_contact: Option[String],
                       cf_v1_reference: Option[String],
                       estimated_time: Int,
                       remaining_time: Int,
                       resolution: Option[String],
                       classification: Option[String],
                       cf_versiononestate: Option[String],
                       status: String,
                       cf_databasestoupdate: Option[String],
                       summary: String,
                       is_open: Boolean,
                       platform: String,
                       cf_hotdeploy_approved: Option[String],
                       flags: Seq[String],
                       cf_production: Option[String],
                       cf_customer_perspective: Option[String],
                       version: String,
                       cf_target_milestone: Option[String],
                       deadline: Option[String],
                       component: Option[String],
                       actual_time: Int,
                       is_creator_accessible: Boolean,
                       product: Option[String],
                       is_confirmed: Boolean,
                       target_milestone: Option[String]
                      )

case class BugzillaParams(Bugzilla_login: String,
                          Bugzilla_password: String,
                          status: List[String],
                          cf_target_milestone: List[String],
                          cf_production: List[String])
object BugzillaParams {
  implicit val encodeFoo: Encoder[BugzillaParams] = deriveEncoder[BugzillaParams]

  def create(username: String,
             password: String,
             statuses: List[String] = List(),
             targets: List[String] = List(),
             environments: List[String] = List("Production")) = new BugzillaParams(username, password, statuses, targets, environments)
}
case class BugzillaRequest(method: String, params: BugzillaParams)
case class BugzillaResponse(error: Option[BugzillaError], id: String, result: Option[BugzillaResult])
case class BugzillaError(message: String, code: Int)
case class BugzillaResult(bugs: Seq[BugzillaBug])