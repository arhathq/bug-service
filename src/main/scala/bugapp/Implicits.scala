package bugapp

import java.time.{LocalDate, OffsetDateTime}

import bugapp.bugzilla._
import bugapp.repository.{Bug, BugHistory, HistoryItem, HistoryItemChange}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Printer}
import io.circe.syntax._

/**
  *
  */
object Implicits {
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] = Encoder.instance(a => a.toString.asJson)
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] = Decoder.instance(a => a.as[String].map(OffsetDateTime.parse(_)))
  implicit val localDateEncoder: Encoder[LocalDate] = Encoder.instance(a => a.toString.asJson)
  implicit val localDateDecoder: Decoder[LocalDate] = Decoder.instance(a => a.as[String].map(LocalDate.parse(_)))

  implicit val encoderBugzillaBug = deriveEncoder[BugzillaBug]
  implicit val decoderBugzillaBug = deriveDecoder[BugzillaBug]
  implicit val decoderBugzillaHistory: Decoder[BugzillaHistory] = deriveDecoder[BugzillaHistory]
  implicit val decoderBugzillaHistoryItem: Decoder[BugzillaHistoryItem] = deriveDecoder[BugzillaHistoryItem]
  implicit val decoderBugzillaHistoryChange: Decoder[BugzillaHistoryChange] = deriveDecoder[BugzillaHistoryChange]
  implicit val encoderBugzillaParams: Encoder[BugzillaParams] = deriveEncoder[BugzillaParams]
  implicit val decoderBugzillaResponse: Decoder[BugzillaResponse] = deriveDecoder[BugzillaResponse]
  implicit val decoderBugzillaError: Decoder[BugzillaError] = deriveDecoder[BugzillaError]
  implicit val decoderBugzillaResult: Decoder[BugzillaResult] = deriveDecoder[BugzillaResult]

  implicit val decoderBug: Decoder[Bug] = deriveDecoder[Bug]
  implicit val encoderBug: Encoder[Bug] = deriveEncoder[Bug]
  implicit val decoderBugHistory: Decoder[BugHistory] = deriveDecoder[BugHistory]
  implicit val encoderBugHistory: Encoder[BugHistory] = deriveEncoder[BugHistory]
  implicit val decoderBugHistoryItem: Decoder[HistoryItem] = deriveDecoder[HistoryItem]
  implicit val encoderBugHistoryItem: Encoder[HistoryItem] = deriveEncoder[HistoryItem]
  implicit val decoderBugHistoryItemChange: Decoder[HistoryItemChange] = deriveDecoder[HistoryItemChange]
  implicit val encoderBugHistoryItemChange: Encoder[HistoryItemChange] = deriveEncoder[HistoryItemChange]

  implicit val jsonPrinter: Printer = Printer(preserveOrder = true, dropNullKeys = true, indent = "")
}
