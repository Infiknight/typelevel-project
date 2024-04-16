package com.rockthejvm.jobsboard.components

import tyrian.CSS.*
import tyrian.*
import tyrian.Html.*

import cats.effect.*
import org.http4s
import org.http4s.client.websocket
import org.http4s.client.websocket.WSDataFrame
import org.http4s.client.websocket.WSFrame.Text
import org.http4s.client.websocket.WSFrame.Binary
import com.rockthejvm.Example
import _root_.io.circe
import monocle.syntax.all.*
import fs2.Stream
import concurrent.duration.DurationInt
import _root_.io.bullet.borer.Cbor
import _root_.io.bullet.borer.compat.scodec.*
import marketData.exchange.impl.binance.domain.Orderbook
import org.http4s.client.websocket.WSFrame
import org.http4s.QueryParamEncoder
import marketData.FeedDefinition
import marketData.FeedDefinition.given
import marketData.Currency
import com.rockthejvm.jobsboard.components.OrderbookView
import com.rockthejvm.jobsboard.components.MarketFeedSelectionStage2
import com.rockthejvm.jobsboard.components.MarketFeedSelectionStage2.SelectExchange
import com.rockthejvm.jobsboard.components.MarketFeedSelectionStage2.SelectFeed
import com.rockthejvm.jobsboard.components.MarketFeedSelectionStage2.SelectCurrency1
import com.rockthejvm.jobsboard.components.MarketFeedSelectionStage2.SelectCurrency2
import com.rockthejvm.jobsboard.components.MarketFeedSelectionStage2.TotalSelection
import com.rockthejvm.jobsboard.App.Displayable
import _root_.io.bullet.borer
import com.rockthejvm.jobsboard.App.UpdateSells
import com.rockthejvm.jobsboard.App.Model
import com.rockthejvm.jobsboard.App.Msg

// object StreamFromServer {
//   def stream(feedName: FeedDefinition[?]): IO[SubscriptionDef] = {
//     http4s
//       .dom.WebSocketClient[IO].connectHighLevel(
//         websocket.WSRequest(
//           uri = http4s
//             .Uri.fromString("ws://127.0.0.1:8080")
//             .map(_.withQueryParam("feedName", feedName: FeedDefinition[?]))
//             .getOrElse(None.get)
//         )
//       ).allocated
//       .map { case (conn, cleanup) =>
//         val receiveStreamTransformed: Stream[IO, Displayable] =
//           Stream.repeatEval(conn.send(WSFrame.Text("")) <* IO.sleep(500.millis)) `zipRight`
//             conn
//               .receiveStream
//               .map {
//                 case Binary(data, _) => Cbor.decode(data).to[feedName.Message](using feedName.borerDecoderForMessage).value
//                 case _ => throw new Exception("unexpected non binary ws frame")
//               }
//               .map {
//                 case feedMessage: (Orderbook | FeedDefinition.Stub.Message) => Displayable(feedMessage)
//                 case _ => throw new Exception("feedMessage neither Orderbook not Stub.Message")
//               }

//         Model.SubscriptionDef("server stream", receiveStreamTransformed, cleanup)
//       }
//   }
// }

object StreamFromServer {
  def stream[M <: Msg](feedName: FeedDefinition[M]): Sub[IO, M] = {
    val undelyingStream: Stream[IO, M] = Stream
      .resource(
        http4s
          .dom.WebSocketClient[IO].connectHighLevel(
            websocket.WSRequest(
              uri = http4s
                .Uri.fromString("ws://127.0.0.1:8080")
                .map(_.withQueryParam("feedName", feedName: FeedDefinition[?]))
                .getOrElse(None.get)
            )
          )
      )
      .flatMap { conn =>
        Stream.repeatEval(conn.send(WSFrame.Text("")) <* IO.sleep(500.millis)) `zipRight`
          conn
            .receiveStream
            .map {
              case Binary(data, _) => Cbor.decode(data).to[M](using feedName.borerDecoderForMessage).value
              case _ => throw new Exception("unexpected non binary ws frame")
            }

        // Model.SubscriptionDef("server stream", receiveStreamTransformed, cleanup)

      }
    Sub.make(feedName.getClass.getSimpleName, undelyingStream)
  }
}
