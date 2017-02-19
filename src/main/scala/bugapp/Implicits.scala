package bugapp

import java.time.{LocalDate, OffsetDateTime}

import bugapp.bugzilla._
import bugapp.repository._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Printer}
import io.circe.syntax._

/**
  *
  */
object Implicits {
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] = Encoder.instance(a => a.asJson)
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] = Decoder.instance(a => a.as[String].right.map(OffsetDateTime.parse(_)))
  implicit val localDateEncoder: Encoder[LocalDate] = Encoder.instance(a => a.asJson)
  implicit val localDateDecoder: Decoder[LocalDate] = Decoder.instance(a => a.as[String].right.map(LocalDate.parse(_)))

  implicit val encoderBugzillaBug = deriveEncoder[BugzillaBug]
  implicit val decoderBugzillaBug = deriveDecoder[BugzillaBug]
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
  implicit val decoderBugStats: Decoder[BugStats] = deriveDecoder[BugStats]
  implicit val encoderBugStats: Encoder[BugStats] = deriveEncoder[BugStats]
  implicit val decoderBugHistory: Decoder[BugHistory] = deriveDecoder[BugHistory]
  implicit val encoderBugHistory: Encoder[BugHistory] = deriveEncoder[BugHistory]
  implicit val decoderBugHistoryItem: Decoder[HistoryItem] = deriveDecoder[HistoryItem]
  implicit val encoderBugHistoryItem: Encoder[HistoryItem] = deriveEncoder[HistoryItem]
  implicit val decoderBugHistoryItemChange: Decoder[HistoryItemChange] = deriveDecoder[HistoryItemChange]
  implicit val encoderBugHistoryItemChange: Encoder[HistoryItemChange] = deriveEncoder[HistoryItemChange]

  implicit val jsonPrinter: Printer = Printer(preserveOrder = true, dropNullKeys = true, indent = "")
}