package bugapp.repository

import bugapp.Bug

import scala.concurrent.Future

trait BugRepository {
  def getBugs: Future[Seq[Bug]]
}

class BugRepositoryImpl extends BugRepository {
  def getBugs: Future[Seq[Bug]] = {
    Future.successful(List(Bug(1, "Bug Name")))
  }
}
