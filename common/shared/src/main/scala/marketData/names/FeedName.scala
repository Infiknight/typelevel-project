package marketData.names

import marketData.domain.Orderbook
import org.http4s
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import _root_.io.scalaland.chimney
import _root_.io.scalaland.chimney.syntax.*
import _root_.io.scalaland.chimney.cats.*
import _root_.io.bullet.borer
import _root_.io.bullet.borer.compat.scodec.*
import _root_.io.bullet.borer.derivation.ArrayBasedCodecs.*
import _root_.io.circe
import _root_.io.circe.generic.semiauto.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import org.http4s.ParseFailure
import org.http4s.QueryParamKeyLike
import org.http4s.QueryParam
import scodec.bits.ByteVector
import scodec.bits.Bases.Alphabets.Base64Url
import java.util.Locale
import marketData.names.Currency
import marketData.domain.Candlestick

sealed trait FeedName[M: borer.Encoder: borer.Decoder] {
  // in retrospect I (maybe) should have used scala 3 Match Types to associate FeedName subtypes with their Message type
  // https://docs.scala-lang.org/scala3/reference/new-types/match-types.html
  // I was indecisive on how many scala 3 features to use
  type Message = M

  def nameWithoutParametersForPrometheusLabelValue: String = this.getClass().getSimpleName()

  def parametersStringForPrometheusLabelValue: String

  def borerEncoderForMessage: borer.Encoder[M] = summon[borer.Encoder[M]]

  def borerDecoderForMessage: borer.Decoder[M] = summon[borer.Decoder[M]]
}

object FeedName {
  case class OrderbookFeed(tradePair: TradePair) extends FeedName[Orderbook] {
    override val parametersStringForPrometheusLabelValue: String = tradePair.base.name ++ tradePair.quote.name
  }

  case class Candlesticks(tradePair: TradePair) extends FeedName[Candlestick] {
    override val parametersStringForPrometheusLabelValue: String = tradePair.base.name ++ tradePair.quote.name
  }

  given http4s.QueryParamCodec[FeedName[?]] = {

    given borer.Codec[FeedName[?]] = deriveAllCodecs[FeedName[?]]

    http4s
      .QueryParamCodec.from(
        decodeA = http4s
          .QueryParamDecoder.stringQueryParamDecoder.emap { string =>
            borer
              .Cbor.decode(ByteVector.fromValidBase64(string, Base64Url)).to[FeedName[?]].valueEither.left
              .map { err =>
                println(err.getMessage)
                ParseFailure(sanitized = err.getMessage, details = err.getMessage)
              }
          },
        encodeA = http4s.QueryParamEncoder.stringQueryParamEncoder.contramap { feedDef =>
          borer.Cbor.encode(feedDef).to[ByteVector].result.toBase64Url
        }
      )
  }

  object Matcher extends QueryParamDecoderMatcher[FeedName[?]]("feedName")
}
