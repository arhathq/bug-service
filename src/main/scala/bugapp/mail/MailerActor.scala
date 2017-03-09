package bugapp.mail

import java.time.OffsetDateTime

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import bugapp.MailerConfig

import scala.util.{Failure, Success, Try}


class MailerActor extends Actor with ActorLogging with MailerConfig {
  import MailerActor._

  private val mailService = new MailServiceImpl(mailUsername.orNull, mailPassword.orNull, mailProps)

  override def receive: Receive = {
    case SendMail(mailMessage) =>

      Try(mailService.sendMessage(mailMessage)) match {
        case Success(_) =>
          log.debug(s"Message ${mailMessage.getId} was sent")
          sender ! MailSent(mailMessage.getId, OffsetDateTime.now)
        case Failure(t) =>
          log.error(t, s"Message ${mailMessage.getId} was not sent")
      }
  }
}

object MailerActor {
  def props() = Props(classOf[MailerActor])

  case class SendMail(mail: MailMessage)
  case class MailSent(mailId: String, date: OffsetDateTime)
}