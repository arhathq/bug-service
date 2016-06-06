package bugapp

case class BugsError(message: String) extends Exception(message)

case class Bug(id: Int,
               priority: String,
               creator: Option[String],
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

case class GetBugsRequest(method: String = "Bug.search", params: Map[String, String])
object GetBugsRequest {
  def apply(username: String, password: String) = new GetBugsRequest("Bug.search", Map("Bugzilla_login" -> username, "Bugzilla_password" -> password))
}

case class GetBugsResponse(error: Option[Error], id: String, result: Option[Result])

case class Error(message: String, code: Int)
case class Result(bugs: Seq[Bug])