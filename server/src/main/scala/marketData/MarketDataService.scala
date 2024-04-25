package marketData

import fs2.Stream
import fs2.concurrent.Topic

import cats.*
import cats.data.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.MapRef
import cats.effect.std.Mutex
import cats.effect.std.AtomicCell

import marketData.exchange.ExchangeSpecific
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import marketData.names.FeedName.OrderbookFeed
import scala.collection.concurrent.TrieMap

import org.http4s.client.websocket.WSClientHighLevel
import org.http4s.jdkhttpclient.JdkWSClient
import cats.effect.std.Queue
import marketData.names.TradePair
import names.FeedName

trait MarketDataService[F[_]] {
  def stream[Message](feed: FeedName[Message]): Stream[F, Message]
  def activeCurrencyPairs: F[List[TradePair]]
}

object MarketDataService {

  /**
   * Responsible for managing the lifecycle of the backing feeds. Avoids over-provisioning when multiple users request the same feed.
   *
   * @param exchangeSpecific
   *   provides the backing feed from the Exchange
   * @param F
   * @return
   */
  def apply[F[_]](exchangeSpecific: ExchangeSpecific[F])(using F: Async[F]): F[MarketDataService[F]] = {

    /**
     * Triple containing what we need to share a data feed among multiple subscribers.
     *
     * @param signal
     *   Reports the updates of the backing data feed
     * @param finalizer
     *   Finalizer to close the backing data feed when subscribersCount reaches 0
     * @param subscribersCount
     *   Number of users currently sharing the backing data feed
     */
    case class SignalFinalizerCount[M](
        signal: Signal[F, Either[Throwable, M]],
        finalizer: F[Unit],
        subscribersCount: Int
    )

    val locks: F[Map[FeedName[?], Mutex[F]]] = exchangeSpecific
      .allFeedNames.traverse { feedDef =>
        Mutex.apply[F].map(feedDef -> _)
      }.map(_.toMap)

    val initSFCs: F[Map[FeedName[?], Ref[F, Option[SignalFinalizerCount[?]]]]] = exchangeSpecific
      .allFeedNames.traverse { feedDef =>
        Ref.of[F, Option[SignalFinalizerCount[?]]](None).map(feedDef -> _)
      }.map(_.toMap)

    val activeSubs: F[MapRef[F, FeedName[?], Option[Int]]] =
      F.delay {
        TrieMap
          .from[FeedName[?], Int](
            exchangeSpecific.allFeedNames.map { _ -> 0 }
          )
      }.map(MapRef.fromScalaConcurrentMap)

    (locks, initSFCs).mapN {
      case (
            locks,
            sfcMap
          ) =>
        def sfc[M](feedDef: FeedName[M]): F[Ref[F, Option[SignalFinalizerCount[M]]]] = {
          F.fromOption(sfcMap.get(feedDef).map(_.asInstanceOf[Ref[F, Option[SignalFinalizerCount[M]]]]), new Exception(""))
        }

        new MarketDataService[F] {

          override def stream[Message](feed: FeedName[Message]): Stream[F, Message] = {
            def listenToAndPotentiallySetupBackingFeed = (poll: Poll[F]) =>
              locks(feed).lock.surround {
                for {
                  sfcRef <- sfc(feed)
                  currentSFC <- sfcRef.get
                  updatesFromBackingFeed <- currentSFC match {
                    case None =>
                      // setup shared backing feed and register its signal and finalizer
                      poll(
                        exchangeSpecific
                          .stream(feed)
                          .attempt // attempt & rethrow later to ensure hold1Resource does not swallow the cause
                          .hold1Resource
                          .allocated
                      )
                        .flatTap { case (signal, finalizer) =>
                          sfcRef.set(Some(SignalFinalizerCount(signal, finalizer, 1)))
                        }.map(_._1)
                    case Some(SignalFinalizerCount(signal, finalizer, count)) =>
                      sfcRef.set(Some(SignalFinalizerCount(signal, finalizer, count + 1))).as(signal)
                  }
                } yield updatesFromBackingFeed.discrete.rethrow
              }

            def potentiallyShutdownBackingFeed = locks(feed).lock.surround {
              for {
                sfcRef <- sfc(feed)
                SignalFinalizerCount(signal, finalizer, count) <- sfcRef
                  .get.map(_.toRight(new Exception("SignalFinalizerCount Map was expected to contain an entry at time of release"))).rethrow
                _ <-
                  if (count == 1) {
                    finalizer *> sfcRef.set(None)
                  } else {
                    sfcRef.set(Some(SignalFinalizerCount(signal, finalizer, count - 1)))
                  }
              } yield ()
            }

            Stream
              .bracketFull(
                acquire = listenToAndPotentiallySetupBackingFeed
              )(
                release = (_, _) => potentiallyShutdownBackingFeed
              ).flatten
          }

          override def activeCurrencyPairs: F[List[TradePair]] = exchangeSpecific.activeCurrencyPairs
        }
    }

  }
}
