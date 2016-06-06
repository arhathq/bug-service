package bugapp.repository

import bugapp.Bug

import scala.concurrent.Future

trait BugRepository {
  def getBugs: Future[Seq[Bug]]
}