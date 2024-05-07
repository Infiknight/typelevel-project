package marketData.exchange.impl.binance

import cats.effect.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import marketData.names.Currency
import marketData.exchange.impl.binance.domain.Orderbook
import cats.effect.*
import org.http4s.client.websocket
import org.http4s
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import scala.concurrent.duration.Duration
import cats.effect.std.Semaphore
import org.http4s.Uri
import marketData.exchange.impl.Binance
import _root_.io.circe
import cats.effect.kernel.Outcome.Succeeded
import cats.effect.kernel.Outcome.Errored
import cats.effect.kernel.Outcome.Canceled
import org.http4s.client.websocket.WSRequest
import fs2.Stream
import org.http4s.client.websocket.WSFrame.Text
import org.http4s.client.websocket.WSFrame.Binary
import client.RateLimitedHttpClient
import client.RateLimitedWSClient
import marketData.names.TradePair
import _root_.io.scalaland.chimney.syntax.*
import _root_.io.scalaland.chimney.cats.*

class Client[F[_]](
    httpClient: RateLimitedHttpClient[F],
    wsClient: RateLimitedWSClient[F]
)(using F: Async[F]) {
  def orderbookSnapshot(tradePair: TradePair): F[Orderbook] =
    httpClient
      .get[dto.Orderbook](
        constants.orderbookSnapshotEndpoint(tradePair).renderString,
        constants.orderbookSnapshotRLPermits
      )
      .map(dto.Orderbook.transformer.transform)

  def orderbookUpdates(tradePair: TradePair): Stream[F, domain.OrderbookUpdate] =
    wsClient
      .wsConnect[dto.OrderbookUpdate](
        constants.diffDepthStreamEndpoint(tradePair).renderString
      ).map(domain.OrderbookUpdate.transformer.transform)
      .evalTap(out => F.delay(println(out.lastUpdateId)))

  def candlesticks(tradePair: TradePair): Stream[F, marketData.domain.Candlestick] =
    wsClient
      .wsConnect[dto.Candlestick](
        constants.candlestickStreamEndpoint(tradePair).renderString
      ).map(_.transformInto[marketData.domain.Candlestick])

}
