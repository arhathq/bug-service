package bugapp.bugzilla

import akka.actor.ActorSystem
import akka.event.{ActorEventBus, LookupClassification}

class RepositoryEventBus(system: ActorSystem) extends ActorEventBus with LookupClassification {
  import RepositoryEventBus._

  type Event = MessageEvent
  type Classifier = MessageType

  override protected def mapSize(): Int = 10

  override protected def classify(event: Event): Classifier = event.messageType

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event.message
}

object RepositoryEventBus {
  trait MessageType
  case object RequestType extends MessageType
  case object ResponseType extends MessageType

  trait MessageEvent {
    val messageType: MessageType
    val message: Message
  }
  case class UpdateRequiredEvent() extends MessageEvent {
    val messageType = RequestType
    val message = UpdateRequired
  }
  case class UpdateGrantedEvent() extends MessageEvent {
    val messageType = ResponseType
    val message = UpdateGranted
  }
  case class UpdateCompletedEvent() extends MessageEvent {
    val messageType = RequestType
    val message = UpdateCompleted
  }

  trait Message
  case object UpdateRequired extends Message
  case object UpdateGranted extends Message
  case object UpdateCompleted extends Message
}