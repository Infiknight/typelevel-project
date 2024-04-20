package marketData.exchange.impl

import marketData.exchange.ExchangeSpecific
import marketData.FeedName
import marketData.names.Currency
import org.http4s.client.websocket.WSRequest
import marketData.FeedName.OrderbookFeed
import marketData.FeedName.Stub
import cats.effect.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import org.http4s
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.Uri
import org.http4s.Request
import org.http4s.dsl.*
import org.http4s.implicits.*
import cats.MonadThrow
import scala.concurrent.duration.*
import binance.dto
import marketData.exchange.impl.binance.domain.RateLimits
import cats.effect.std.Semaphore
import fs2.{Stream, Pull}
import fs2.concurrent.Signal
import org.http4s.client.websocket.WSFrame.Text
import org.http4s.client.websocket.WSFrame.Binary
import _root_.io.circe
import marketData.exchange.impl.binance.domain.{OrderbookUpdate, Orderbook, RateLimits}
import binance.dto.ExchangeInfo.SymbolPair.Status
import marketData.names.TradePair
import marketData.exchange.impl.binance.dto.ExchangeInfo
import _root_.io.scalaland.chimney.syntax.*

trait Binance[F[_]] private (
    client2: binance.Client[F]
)(
    implicit F: Async[F]
) extends ExchangeSpecific[F] {

  override def stream[M](feedDef: FeedName[M]): Stream[F, M] = feedDef match {
    case orderbookFeedDef: FeedName.OrderbookFeed => orderbookStream(orderbookFeedDef)
    case Stub(_value) => ???
  }

  // Assumption: ws market stream messages are guaranteed to arrive in order
  def orderbookStream(level2Def: FeedName.OrderbookFeed): fs2.Stream[F, Orderbook] = level2Def match {
    case OrderbookFeed(currency1, currency2) =>
      /**
       * We assume there are no messages missing or out of order
       */
      val orderbookUpdates: Stream[F, OrderbookUpdate] = client2.orderbookUpdates(currency1 = currency1, currency2 = currency2)

      val orderbookSnapshot: F[Orderbook] = client2.orderbookSnapshot(currency1 = currency1, currency2 = currency2)

      // TODO check if expressible using Stream#debounce
      val orderbookSnapshotsAsNestedPull = for {
        firstUpdateMaybe <- orderbookUpdates.pull.uncons1
        (firstUpdate, rest) <- Pull.eval(F.fromOption(firstUpdateMaybe, new Exception))
        snapshotIssuedAfterFirstUpdate <- Pull.eval(orderbookSnapshot)
        relevantUpdates = rest.dropWhile(_.lastUpdateId <= snapshotIssuedAfterFirstUpdate.lastUpdateId)
        orderbookSnapshots = relevantUpdates
          .scan(snapshotIssuedAfterFirstUpdate) { case (snapshot, update) =>
            update.update(snapshot)
          }.pull.echo
      } yield orderbookSnapshots

      orderbookSnapshotsAsNestedPull.flatten.stream
  }
}

object Binance {
  val baseEndpoint = uri"https://api.binance.com"
  val exchangeInfoUriSegment: String = "api/v3/exchangeInfo"
  val exchangeInfoRequestWeight = 20
  val wsConnectionPermits = 300
  val wsConnectionPermitReleaseTime = 5.minutes
  def symbol(currency: Currency): String = currency.name
  def streamSymbol(currency: Currency): String = currency.name.toLowerCase()
  def tradePairSymbol(c1: Currency, c2: Currency): String = symbol(c1) ++ symbol(c2)
  def streamTradePairSymbol(c1: Currency, c2: Currency): String = streamSymbol(c1) ++ streamSymbol(c2)

  def apply[F[_]](
      http4sHttpClient: http4s.client.Client[F],
      wsClient: http4s.client.websocket.WSClientHighLevel[F]
  )(
      using F: Async[F]
  ): F[Binance[F]] = {
    for {
      exchangeInfo <- http4sHttpClient
        .expect[dto.ExchangeInfo](baseEndpoint.addPath(exchangeInfoUriSegment))
      RateLimits(requestWeight, _) <- F.fromEither(RateLimits.of(exchangeInfo))

      httpRateLimitSem <- Semaphore(requestWeight.permitsAvailable - exchangeInfoRequestWeight)
      wsConnectionRateLimitSem <- Semaphore(wsConnectionPermits)
      binanceHttpClient = client
        .HttpClient.HttpClientLive(
          httpClient = http4sHttpClient,
          rateLimitsData = client
            .rateLimits.RLSemaphoreAndReleaseTime(
              semaphore = httpRateLimitSem,
              releaseTime = requestWeight.timeToReleasePermits
            )
        )
      binanceWSClient = client
        .WSClient.WSCLientLive(
          wsClient = wsClient,
          wsEstablishConnectionRL = client
            .rateLimits.RLSemaphoreAndReleaseTime(
              semaphore = wsConnectionRateLimitSem,
              releaseTime = wsConnectionPermitReleaseTime
            )
        )
    } yield new Binance(binance.Client(binanceHttpClient, binanceWSClient)) {

      override def activeCurrencyPairs: F[List[TradePair]] =
        binanceHttpClient
          .get[ExchangeInfo](baseEndpoint.addPath(exchangeInfoUriSegment).renderString, exchangeInfoRequestWeight)
          .map(
            _.symbols
              .filter(_.status == Status.TRADING)
              .map { pair => pair.transformInto[TradePair] }
          )

      override def allCurrencyPairs: List[(Currency, Currency)] =
        exchangeInfo
          .symbols
          .filter(_.status == Status.TRADING) // TODO remove
          .map { pair => pair.baseAssetCurrency -> pair.quoteAssetCurrency }
    }
  }
}
