package marketData.exchange

import cats.*
import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import marketData.names.Currency
import marketData.names.FeedName
import marketData.names.FeedName.Candlesticks
import marketData.names.FeedName.OrderbookFeed
import marketData.names.TradePair
import names.ExchangeName

trait Exchange[F[_]: Async] {
  def allCurrencyPairs: List[TradePair]

  def activeCurrencyPairs: F[List[TradePair]]

  def allFeedNames: List[FeedName[?]] = {
    val allLevel2Names: List[FeedName.OrderbookFeed] = allCurrencyPairs.map(FeedName.OrderbookFeed.apply)
    val allCandlestickNames: List[FeedName.Candlesticks] = allCurrencyPairs.map(FeedName.Candlesticks.apply)

    allLevel2Names.prependedAll(allCandlestickNames) // plus others
  }

  def stream[M](feedName: FeedName[M]): Stream[F, M]

  def name: ExchangeName
}

object Exchange {
  def stub[F[_], M](using F: Async[F])(
      allCurrencyPairsStub: List[TradePair] = List(TradePair(base = Currency("BTC"), quote = Currency("ETH"))),
      activeCurrencyPairsStub: F[List[TradePair]] = List(TradePair(base = Currency("BTC"), quote = Currency("ETH"))).pure[F],
      streamStub: (feedName: FeedName[?]) => Stream[F, feedName.Message] = { _ => Stream.raiseError(new UnsupportedOperationException) },
      nameStub: ExchangeName = ExchangeName.Binance
  ): Exchange[F] = new Exchange {

    override def allCurrencyPairs: List[TradePair] = allCurrencyPairsStub

    override def activeCurrencyPairs: F[List[TradePair]] = activeCurrencyPairsStub

    override def stream[M](feedDef: FeedName[M]): Stream[F, M] = streamStub(feedDef)

    override def name: ExchangeName = nameStub

  }
}
