package bugapp.repository

import bugapp.Bug

import scala.concurrent.Future

trait BugRepository {
  def getBugs(statuses: List[String] = List(), milestones: List[String] = List(), environments: List[String]= List()): Future[Seq[Bug]]
}