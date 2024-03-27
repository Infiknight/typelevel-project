package marketData

import weaver.SimpleIOSuite
import cats.effect.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import marketData.backingStreams.BackingStreamsService
import fs2.Stream
import marketData.FeedDefinition.OrderbookFeed
import marketData.FeedDefinition.Stub
import marketData.exchange.ExchangeSpecific

object MarketDataServiceSpec extends SimpleIOSuite {
  val backingStreamsAndCallCount = Ref.of[IO, Int](0).map { ref =>
    // def emitStubMessagesForever[Message](stubFeed: FeedDefinition[Message]): Stream[IO, Message] =
    //   Stream.iterate(start = 0)(_ + 1).map(FeedDefinition.Stub.Message.apply).covary[IO]

    val backingStreams = new ExchangeSpecific[IO] {

      override def allCurrencyPairs: List[(Currency, Currency)] = List(
        Currency("BTC") -> Currency("ETH"),
        Currency("ETH") -> Currency("BTC")
      )

      override def stream[Message](feed: FeedDefinition[Message]): Stream[IO, Message] = feed match {
        case OrderbookFeed(currency1, currency2) => ???
        case stubFeed: Stub =>
          Stream
            .eval(ref.update(_ + 1)) >> Stream
            .iterate(start = 0)(_ + 1).map(FeedDefinition.Stub.Message.apply)
            .covary[IO]
        // .evalTap(IO.print)
      }

    }

    backingStreams -> ref
  }

  test("reuses the backing stream when two requests overlap") {
    // val exchangeParamsStub = ExchangeSpecific.stub
    for {
      (bStreams, callCount) <- backingStreamsAndCallCount
      marketDataService <- MarketDataService.apply(bStreams)
      requestStubDatafeed = marketDataService.stream(bStreams.allFeedDefs.head)
      _ <- requestStubDatafeed.zip(requestStubDatafeed).take(10).compile.toList.map(print)
      callCountV <- callCount.get
    } yield expect(callCountV == 1)
  }
}