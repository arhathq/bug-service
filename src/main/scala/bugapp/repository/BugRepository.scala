package bugapp.repository

import bugapp.{Bug, Post}

import scala.concurrent.Future

trait BugRepository {
  def getBugs: Future[Seq[Bug]]
}