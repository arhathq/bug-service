package bugapp.stream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import io.circe.jawn.CirceSupportParser._
import io.circe.{CursorOp, Decoder, Encoder, HCursor, Json, Printer}
import jawn.AsyncParser

import scala.annotation.tailrec
import scala.language.implicitConversions

object CirceStreamSupport extends CirceStreamSupport

trait CirceStreamSupport {

  def decode[A: Decoder]: Flow[ByteString, A, NotUsed] =
    JsonStreamParser.flow[Json].map(decodeJson[A])

  def decode[A: Decoder](mode: AsyncParser.Mode): Flow[ByteString, A, NotUsed] =
    JsonStreamParser.flow[Json](mode).map(decodeJson[A])

  def encode[A](implicit A: Encoder[A], P: Printer = Printer.noSpaces): Flow[A, String, NotUsed] =
    Flow[A].map(a ⇒ P.pretty(A(a)))

  private[stream] def decodeJson[A](json: Json)(implicit decoder: Decoder[A]): A = {
    val cursor = json.hcursor
    decoder(cursor) match {
      case Right(e) ⇒ e
      case Left(f)  ⇒ throw new IllegalArgumentException(errorMessage(f.history, cursor, f.message))
    }
  }


  private[this] def errorMessage(hist: List[CursorOp], cursor: HCursor, typeHint: String) = {
    val field = fieldFromHistory(hist)
    val down = cursor.downField(field)
    if (down.succeeded) {
      s"Could not decode [${down.focus.get}] at [$field] as [$typeHint]."
    } else {
      s"The field [$field] is missing."
    }
  }

  @tailrec
  private[this] def fieldFromHistory(hist: List[CursorOp], arrayIndex: Int = 0, out: List[String] = Nil): String = hist match {
    case some :: rest ⇒ some match {
      case CursorOp.MoveRight    ⇒ fieldFromHistory(rest, arrayIndex + 1, out)
      case CursorOp.MoveLeft     ⇒ fieldFromHistory(rest, arrayIndex - 1, out)
      case CursorOp.DownArray    ⇒ fieldFromHistory(rest, 0, s"[$arrayIndex]" :: out)
      case CursorOp.DownField(f) ⇒ fieldFromHistory(rest, arrayIndex, f :: out)
      case _                           ⇒ fieldFromHistory(rest, arrayIndex, out)
    }
    case Nil          ⇒ out.mkString(".")
  }
}
