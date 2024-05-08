package marketData.exchange.impl.coinbase

import cats.effect.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import mouse.all.*
import fs2.{Stream, Pull}
import client.RateLimitedWSClient
import client.RateLimitedHttpClient
import marketData.names.TradePair
import org.http4s.implicits.uri
import org.http4s
import marketData.exchange.impl.coinbase.dto.Level2Message.Relevant
import marketData.exchange.impl.coinbase.dto
import _root_.io.scalaland.chimney.syntax.*
import _root_.io.scalaland.chimney.cats.*
import marketData.domain.Orderbook
import client.rateLimits.RLSemaphoreAndReleaseTime
import cats.effect.std.Semaphore
import org.typelevel.log4cats.Logger
import marketData.exchange.impl.coinbase.dto.SubscribeRequest
import marketData.names.FeedName
import _root_.io.circe
import marketData.exchange.impl.coinbase.dto.Level2Message.Relevant.Event.Update.Side
import monocle.syntax.all.*
import client.RateLimitedHttpClient.RateLimitedHttpClientLive
import marketData.exchange.impl.coinbase.dto.ListProducts
import marketData.names.Currency

class Client[F[_]: Async] private (
    wsClient: RateLimitedWSClient[F],
    httpClient: RateLimitedHttpClient[F]
) {

  def level2Messages(feedName: FeedName.OrderbookFeed): fs2.Stream[F, dto.Level2Message] = {
    val subscribeRequests =
      SubscribeRequest
        .relevantAndHeartbeats(
          feedName = feedName
        ).map(circe.Encoder.apply[SubscribeRequest].apply)

    wsClient
      .wsConnect[dto.Level2Message](uri = constants.advancedTradeWebSocketEndpoint, subscriptionMessages = subscribeRequests)
  }

  def candlesticks(feedName: FeedName.Candlesticks): Stream[F, dto.CandlesMessage] = {
    val subscribeRequests = SubscribeRequest
      .relevantAndHeartbeats(
        feedName = feedName
      ).map(circe.Encoder.apply[SubscribeRequest].apply)

    wsClient
      .wsConnect[dto.CandlesMessage](
        uri = constants.advancedTradeWebSocketEndpoint,
        subscriptionMessages = subscribeRequests
      )
  }

  def enabledTradePairs: F[List[TradePair]] = httpClient
    .get[ListProducts](
      uri = constants.listPublicProductsEndpoint,
      permitsNeeded = 1
    ).map(_.products)
    .map { products =>
      products
        .filter(!_.is_disabled).map { product => Currency(product.base_currency_id) -> Currency(product.quote_currency_id) }
        .map(TradePair.apply)
    }
}

object Client {
  def apply[F[_]: Async: Logger](
      wsClient: http4s.client.websocket.WSClientHighLevel[F],
      http4sHttpClient: http4s.client.Client[F]
  ): F[Client[F]] = {
    val wsClientWrapped: F[RateLimitedWSClient[F]] = Semaphore(constants.websocketRequestsPerSecondPerIP)
      .map { sem =>
        RLSemaphoreAndReleaseTime(semaphore = sem, releaseTime = constants.websocketRateLimitRefreshPeriod)
      }.map { wsEstablishConnectionRL =>
        RateLimitedWSClient
          .apply(
            wsClient = wsClient,
            wsEstablishConnectionRL = wsEstablishConnectionRL
          )
      }

    val httpClientWrapped: F[RateLimitedHttpClient[F]] = Semaphore(constants.httpRequestsPerSecondPerIP)
      .map { sem =>
        RLSemaphoreAndReleaseTime(semaphore = sem, releaseTime = constants.httpRateLimitRefreshPeriod)
      }.map { rateLimitsData =>
        RateLimitedHttpClientLive(
          httpClient = http4sHttpClient,
          rateLimitsData = rateLimitsData
        )
      }

    (wsClientWrapped, httpClientWrapped).mapN { case (wsClientWrapped, httpClientWrapped) =>
      new Client(
        wsClient = wsClientWrapped,
        httpClient = httpClientWrapped
      )
    }
  }
}
