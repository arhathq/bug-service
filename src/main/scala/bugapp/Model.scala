package bugapp

case class Bug(id: String,
               priority: String,
               blocks: String,
               creator: String,
               last_change_time: String,
               cf_project_team: String,
               is_cc_accessible: Boolean,
               keywords: Array[String],
               cc: Array[String],
               url: String,
               assigned_to: String,
               groups: Array[String],
               see_also: Array[String],
               creation_time: String,
               whiteboard: String,
               qa_contact: String,
               depends_on: Array[String],
               dupe_of: String,
               cf_v1_reference: String,
               estimated_time: Int,
               remaining_time: Int,
               update_token: String,
               resolution: String,
               classification: String,
               alias: String,
               cf_versiononestate: String,
               op_sys: String,
               status: String,
               cf_databasestoupdate: String,
               summary: String,
               is_open: Boolean,
               platform: String,
               severity: String,
               cf_hotdeploy_approved: String,
               flags: Array[String],
               cf_production: String,
               cf_customer_perspective: String,
               version: String,
               cf_target_milestone: String,
               deadline: String,
               component: String,
               actual_time: Int,
               is_creator_accessible: Boolean,
               product: String,
               is_confirmed: Boolean,
               target_milestone: String
              )


case class GetBugsRequest(method: String, params: String)
case class GetBugsResponse(error: Error, id: String, result: Bug)
case class Error(message: String, code: Int)