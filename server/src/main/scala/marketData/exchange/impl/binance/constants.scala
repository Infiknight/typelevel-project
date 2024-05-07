package marketData.exchange.impl.binance

import scala.concurrent.duration.DurationInt
import marketData.names.TradePair
import cats.effect.*
import org.http4s.implicits.uri
import org.http4s.Uri
import scala.concurrent.duration.FiniteDuration
import marketData.names.Currency
import java.util.Locale

object constants {
  // 6 May 2024
  // https://binance-docs.github.io/apidocs/spot/en/#general-info
  val baseEndpoint: Uri = uri"https://api.binance.com"
  val exchangeInfoEndpoint = baseEndpoint.addPath("api/v3/exchangeInfo")

  // https://binance-docs.github.io/apidocs/spot/en/#order-book
  val orderbookSnapshotRLPermits = 250
  def orderbookSnapshotEndpoint(pair: TradePair) =
    baseEndpoint
      .addPath("api/v3/depth")
      .withQueryParam(key = "symbol", value = tradePairSymbol(pair))
      .withQueryParam(key = "limit", value = 5000)

  // https://binance-docs.github.io/apidocs/spot/en/#exchange-information
  val exchangeInfoRequestWeight = 20

  // https://binance-docs.github.io/apidocs/spot/en/#limits
  val wsConnectionPermits = 300
  val wsConnectionPermitReleaseTime = 5.minutes

  def tradePairSymbol(pair: TradePair): String = pair.base.name ++ pair.quote.name

  // https://binance-docs.github.io/apidocs/spot/en/#websocket-market-streams
  val baseWSEndpoint = uri"wss://stream.binance.com:9443/ws"
  def streamSymbol(currency: Currency): String = currency.name.toLowerCase(Locale.ROOT)
  def streamTradePairSymbol(pair: TradePair): String = streamSymbol(pair.base) ++ streamSymbol(pair.quote)

  // https://binance-docs.github.io/apidocs/spot/en/#diff-depth-stream
  def diffDepthStreamEndpoint(tradePair: TradePair) =
    baseWSEndpoint.addSegment(s"${streamTradePairSymbol(tradePair)}@depth@100ms")

  // https://binance-docs.github.io/apidocs/spot/en/#kline-candlestick-streams
  def candlestickStreamEndpoint(tradePair: TradePair) =
    baseWSEndpoint.addSegment(s"${streamTradePairSymbol(tradePair)}@kline_1s")

}