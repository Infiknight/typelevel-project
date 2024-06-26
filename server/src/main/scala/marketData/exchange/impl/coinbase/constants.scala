package marketData.exchange.impl.coinbase

import marketData.names.TradePair
import org.http4s.Uri
import org.http4s.implicits.uri

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

object constants {
  // 26 April 2024
  // https://docs.cloud.coinbase.com/exchange/docs/websocket-rate-limits
  val websocketRequestsPerSecondPerIP: Int = 8
  val websocketRateLimitRefreshPeriod: FiniteDuration = 1.second
  def product_ids(tradePair: TradePair): String = tradePair.base.name ++ "-" ++ tradePair.quote.name

  // 30 April 2024
  // https://docs.cloud.coinbase.com/advanced-trade/docs/ws-overview
  val advancedTradeWebSocketEndpoint = uri"wss://advanced-trade-ws.coinbase.com"

  // 29 April 2024
  // https://docs.cloud.coinbase.com/advanced-trade/docs/rest-api-overview
  val advancedTradeEndpointURL: Uri = uri"https://api.coinbase.com/api/v3/brokerage"
  val httpRequestsPerSecondPerIP: Int = 9 // minus one to accommodate the setup request sent before permit counting
  val httpRateLimitRefreshPeriod: FiniteDuration = 1.second

  // https://docs.cloud.coinbase.com/advanced-trade/reference/retailbrokerageapi_getpublicproducts
  val listPublicProductsEndpoint = advancedTradeEndpointURL.addPath("market/products")
}
