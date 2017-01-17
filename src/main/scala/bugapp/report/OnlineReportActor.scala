package bugapp.report

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import bugapp.bugzilla.RepositoryEventBus
import bugapp.repository.BugRepository

/**
  * Created by arhathq on 16.01.2017.
  */
class OnlineReportActor(bugRepository: BugRepository, repositoryEventBus: RepositoryEventBus) extends Actor with ActorLogging {
  import OnlineReportActor._

  override def receive: Receive = ???
}

object OnlineReportActor {
  def props(bugRepository: BugRepository, repositoryEventBus: RepositoryEventBus) =
    Props(classOf[OnlineReportActor], bugRepository, repositoryEventBus)


}


