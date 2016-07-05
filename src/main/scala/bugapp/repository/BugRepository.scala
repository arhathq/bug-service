package bugapp.repository


import scala.concurrent.Future

trait BugRepository {
  def getBugs(statuses: List[String] = List(), milestones: List[String] = List(), environments: List[String]= List()): Future[Seq[Bug]]
}

case class Bug(id: String,
               severity: String,
               priority: String,
               status: String,
               resolution: String,
               reporter: String,
               opened: String,
               assignee: String,
               changed: String,
               product: String,
               component: String,
               environment: String,
               summary: String,
               hardware: String
              )

case class BugsError(message: String) extends Exception(message)