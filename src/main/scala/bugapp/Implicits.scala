package bugapp

import java.time.{LocalDate, OffsetDateTime}

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.syntax._

/**
  *
  */
object Implicits {
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] = Encoder.instance(a => a.toString.asJson)
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] = Decoder.instance(a => a.as[String].map(OffsetDateTime.parse(_)))

  implicit val localDateEncoder: Encoder[LocalDate] = Encoder.instance(a => a.toString.asJson)
  implicit val localDateDecoder: Decoder[LocalDate] = Decoder.instance(a => a.as[String].map(LocalDate.parse(_)))
}
