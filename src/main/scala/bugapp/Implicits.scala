package bugapp

import java.time.{LocalDate, OffsetDateTime}

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.ContentDispositionTypes
import bugapp.bugzilla._
import bugapp.report.ReportActor.{Report, ReportError}
import bugapp.report.ReportSender.MailDetails
import bugapp.repository._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Printer}
import io.circe.syntax._

/**
  *
  */
object Implicits {
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] = Encoder.instance(a => a.toString.asJson)
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] = Decoder.instance(a => a.as[String].right.map(OffsetDateTime.parse(_)))
  implicit val localDateEncoder: Encoder[LocalDate] = Encoder.instance(a => a.toString.asJson)
  implicit val localDateDecoder: Decoder[LocalDate] = Decoder.instance(a => a.as[String].right.map(LocalDate.parse(_)))

  implicit val encoderBugzillaBug: Encoder[BugzillaBug] = deriveEncoder[BugzillaBug]
  implicit val decoderBugzillaBug: Decoder[BugzillaBug] = deriveDecoder[BugzillaBug]
  implicit val encoderBugzillaHistory: Encoder[BugzillaHistory] = deriveEncoder[BugzillaHistory]
  implicit val decoderBugzillaHistory: Decoder[BugzillaHistory] = deriveDecoder[BugzillaHistory]
  implicit val encoderBugzillaHistoryItem: Encoder[BugzillaHistoryItem] = deriveEncoder[BugzillaHistoryItem]
  implicit val decoderBugzillaHistoryItem: Decoder[BugzillaHistoryItem] = deriveDecoder[BugzillaHistoryItem]
  implicit val encoderBugzillaHistoryChange: Encoder[BugzillaHistoryChange] = deriveEncoder[BugzillaHistoryChange]
  implicit val decoderBugzillaHistoryChange: Decoder[BugzillaHistoryChange] = deriveDecoder[BugzillaHistoryChange]
  implicit val encoderBugzillaParams: Encoder[BugzillaParams] = deriveEncoder[BugzillaParams]
  implicit val decoderBugzillaResponse: Decoder[BugzillaResponse[BugzillaResult]] = deriveDecoder[BugzillaResponse[BugzillaResult]]
  implicit val decoderBugzillaHistResponse: Decoder[BugzillaResponse[BugzillaHistoryResult]] = deriveDecoder[BugzillaResponse[BugzillaHistoryResult]]
  implicit val decoderBugzillaError: Decoder[BugzillaError] = deriveDecoder[BugzillaError]
  implicit val decoderBugzillaResult: Decoder[BugzillaResult] = deriveDecoder[BugzillaResult]
  implicit val decoderBugzillaHistResult: Decoder[BugzillaHistoryResult] = deriveDecoder[BugzillaHistoryResult]

  implicit val decoderBug: Decoder[Bug] = deriveDecoder[Bug]
  implicit val encoderBug: Encoder[Bug] = deriveEncoder[Bug]

  implicit val encoderBugCreatedEvent: Encoder[BugCreatedEvent] = deriveEncoder[BugCreatedEvent]
  implicit val decoderBugCreatedEvent: Decoder[BugCreatedEvent] = deriveDecoder[BugCreatedEvent]
  implicit val encoderBugResolvedEvent: Encoder[BugResolvedEvent] = deriveEncoder[BugResolvedEvent]
  implicit val decoderBugResolvedEvent: Decoder[BugResolvedEvent] = deriveDecoder[BugResolvedEvent]
  implicit val encoderBugClosedEvent: Encoder[BugClosedEvent] = deriveEncoder[BugClosedEvent]
  implicit val decoderBugClosedEvent: Decoder[BugClosedEvent] = deriveDecoder[BugClosedEvent]
  implicit val encoderBugReopenedEvent: Encoder[BugReopenedEvent] = deriveEncoder[BugReopenedEvent]
  implicit val decoderBugReopenedEvent: Decoder[BugReopenedEvent] = deriveDecoder[BugReopenedEvent]
  implicit val encoderBugInProgressEvent: Encoder[BugInProgressEvent] = deriveEncoder[BugInProgressEvent]
  implicit val decoderBugInProgressEvent: Decoder[BugInProgressEvent] = deriveDecoder[BugInProgressEvent]
  implicit val encoderBugResolutionChangedEvent: Encoder[BugResolutionChangedEvent] = deriveEncoder[BugResolutionChangedEvent]
  implicit val decoderBugResolutionChangedEvent: Decoder[BugResolutionChangedEvent] = deriveDecoder[BugResolutionChangedEvent]
  implicit val encoderBugAssignedEvent: Encoder[BugAssignedEvent] = deriveEncoder[BugAssignedEvent]
  implicit val decoderBugAssignedEvent: Decoder[BugAssignedEvent] = deriveDecoder[BugAssignedEvent]
  implicit val encoderBugSubscriberAddedEvent: Encoder[BugSubscriberAddedEvent] = deriveEncoder[BugSubscriberAddedEvent]
  implicit val decoderBugSubscriberAddedEvent: Decoder[BugSubscriberAddedEvent] = deriveDecoder[BugSubscriberAddedEvent]
  implicit val encoderBugPriorityChangedEvent: Encoder[BugPriorityChangedEvent] = deriveEncoder[BugPriorityChangedEvent]
  implicit val decoderBugPriorityChangedEvent: Decoder[BugPriorityChangedEvent] = deriveDecoder[BugPriorityChangedEvent]
  implicit val encoderBugEscalatedEvent: Encoder[BugEscalatedEvent] = deriveEncoder[BugEscalatedEvent]
  implicit val decoderBugEscalatedEvent: Decoder[BugEscalatedEvent] = deriveDecoder[BugEscalatedEvent]
  implicit val encoderBugBlockedEvent: Encoder[BugBlockedEvent] = deriveEncoder[BugBlockedEvent]
  implicit val decoderBugBlockedEvent: Decoder[BugBlockedEvent] = deriveDecoder[BugBlockedEvent]
  implicit val encoderBugVerifiedEvent: Encoder[BugVerifiedEvent] = deriveEncoder[BugVerifiedEvent]
  implicit val decoderBugVerifiedEvent: Decoder[BugVerifiedEvent] = deriveDecoder[BugVerifiedEvent]
  implicit val encoderBugSeverityChangedEvent: Encoder[BugSeverityChangedEvent] = deriveEncoder[BugSeverityChangedEvent]
  implicit val decoderBugSeverityChangedEvent: Decoder[BugSeverityChangedEvent] = deriveDecoder[BugSeverityChangedEvent]
  implicit val encoderBugComponentChangedEvent: Encoder[BugComponentChangedEvent] = deriveEncoder[BugComponentChangedEvent]
  implicit val decoderBugComponentChangedEvent: Decoder[BugComponentChangedEvent] = deriveDecoder[BugComponentChangedEvent]
  implicit val encoderBugMarkedAsProductionEvent: Encoder[BugMarkedAsProductionEvent] = deriveEncoder[BugMarkedAsProductionEvent]
  implicit val decoderBugMarkedAsProductionEvent: Decoder[BugMarkedAsProductionEvent] = deriveDecoder[BugMarkedAsProductionEvent]
  implicit val encoderBugCommentedEvent: Encoder[BugCommentedEvent] = deriveEncoder[BugCommentedEvent]
  implicit val decoderBugCommentedEvent: Decoder[BugCommentedEvent] = deriveDecoder[BugCommentedEvent]
  implicit val encoderBugReadyForTestingEvent: Encoder[BugReadyForTestingEvent] = deriveEncoder[BugReadyForTestingEvent]
  implicit val decoderBugReadyForTestingEvent: Decoder[BugReadyForTestingEvent] = deriveDecoder[BugReadyForTestingEvent]
  implicit val encoderBugTestingInProgressEvent: Encoder[BugTestingInProgressEvent] = deriveEncoder[BugTestingInProgressEvent]
  implicit val decoderBugTestingInProgressEvent: Decoder[BugTestingInProgressEvent] = deriveDecoder[BugTestingInProgressEvent]

  implicit val encoderBugEvent: Encoder[BugEvent] = Encoder.instance {
    case e @ BugCreatedEvent(_, _, _, _) => e.asJson
    case e @ BugResolvedEvent(_, _, _, _) => e.asJson
    case e @ BugReadyForTestingEvent(_, _, _, _) => e.asJson
    case e @ BugTestingInProgressEvent(_, _, _, _) => e.asJson
    case e @ BugClosedEvent(_, _, _, _) => e.asJson
    case e @ BugReopenedEvent(_, _, _, _) => e.asJson
    case e @ BugInProgressEvent(_, _, _, _) => e.asJson
    case e @ BugResolutionChangedEvent(_, _, _, _) => e.asJson
    case e @ BugAssignedEvent(_, _, _, _, _) => e.asJson
    case e @ BugSubscriberAddedEvent(_, _, _, _) => e.asJson
    case e @ BugPriorityChangedEvent(_, _, _, _) => e.asJson
    case e @ BugEscalatedEvent(_, _, _, _) => e.asJson
    case e @ BugBlockedEvent(_, _, _, _) => e.asJson
    case e @ BugVerifiedEvent(_, _, _, _) => e.asJson
    case e @ BugSeverityChangedEvent(_, _, _, _) => e.asJson
    case e @ BugComponentChangedEvent(_, _, _, _, _) => e.asJson
    case e @ BugMarkedAsProductionEvent(_, _, _, _) => e.asJson
    case e @ BugCommentedEvent(_, _, _, _,_) => e.asJson
  }
  implicit val decoderBugEvent: Decoder[BugEvent] = {
    Decoder[BugCreatedEvent].map[BugEvent](identity).
      or(Decoder[BugResolvedEvent].map[BugEvent](identity)).
      or(Decoder[BugReadyForTestingEvent].map[BugEvent](identity)).
      or(Decoder[BugTestingInProgressEvent].map[BugEvent](identity)).
      or(Decoder[BugClosedEvent].map[BugEvent](identity)).
      or(Decoder[BugReopenedEvent].map[BugEvent](identity)).
      or(Decoder[BugInProgressEvent].map[BugEvent](identity)).
      or(Decoder[BugResolutionChangedEvent].map[BugEvent](identity)).
      or(Decoder[BugAssignedEvent].map[BugEvent](identity)).
      or(Decoder[BugSubscriberAddedEvent].map[BugEvent](identity)).
      or(Decoder[BugPriorityChangedEvent].map[BugEvent](identity)).
      or(Decoder[BugEscalatedEvent].map[BugEvent](identity)).
      or(Decoder[BugBlockedEvent].map[BugEvent](identity)).
      or(Decoder[BugVerifiedEvent].map[BugEvent](identity)).
      or(Decoder[BugSeverityChangedEvent].map[BugEvent](identity)).
      or(Decoder[BugComponentChangedEvent].map[BugEvent](identity)).
      or(Decoder[BugMarkedAsProductionEvent].map[BugEvent](identity)).
      or(Decoder[BugCommentedEvent].map[BugEvent](identity))
  }

  implicit val decoderMailDetails: Decoder[MailDetails] = deriveDecoder[MailDetails]
  implicit val encoderMailDetails: Encoder[MailDetails] = deriveEncoder[MailDetails]

  implicit val jsonPrinter: Printer = Printer(preserveOrder = true, dropNullKeys = true, indent = "")

  implicit val reportEncoder: Encoder[ReportError] = deriveEncoder[ReportError]

  implicit def reportMarshaller: ToResponseMarshaller[Either[ReportError, Report]] =  Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/pdf`) {
      case Left(error) =>
        HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), reportEncoder.apply(error).toString()), status = InternalServerError)
      case Right(report) =>
        val cth = headers.`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> report.name))
        HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/pdf`), report.data), headers = List(cth))
    }
  )
}