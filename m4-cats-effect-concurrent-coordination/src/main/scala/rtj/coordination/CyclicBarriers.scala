package rtj.coordination

import cats.effect.{Concurrent, Deferred, IO, IOApp, Ref}
import cats.syntax.all.*
import rtj.all.*

import scala.util.Random

object CyclicBarriers extends IOApp.Simple {

  //type CyclicBarrier[F[_]] = cats.effect.std.CyclicBarrier[F]
  //val CyclicBarrier = cats.effect.std.CyclicBarrier
  type CyclicBarrier[F[_]] = my.CyclicBarrier[F]
  val CyclicBarrier: my.CyclicBarrier.type = my.CyclicBarrier

  /*
      A cyclic barrier is a coordination primitive that
      - is initialized with a count
      - has a single API: await

      A cyclic barrier will (semantically) block all fibers calling its await() method until we have exactly N fibers waiting,
      at which point the barrier will unblock all fibers and reset to its original state.

      Any further fiber will again block until we have exactly N fibers waiting.
      ...
      And so on.
   */

  // example: signing up for a social network just about to be launched
  def createUser(id: Int, barrier: CyclicBarrier[IO]): IO[Unit] = for {
    _ <- IO.sleep(Random.nextInt(500).millis)
    _ <- IO.dbg(s"[user $id] Just heard there's a new social network - signing up for the waitlist...")
    _ <- IO.sleep(Random.nextInt(500).millis)
    _ <- IO.dbg(s"[user $id] On the waitlist now, can't wait!")
    _ <- barrier.await
    _ <- IO.dbg(s"[user $id] OMG this is so cool!")
  } yield ()

  def openNetwork(): IO[Unit] = for {
    _ <- IO.dbg("[announcer] The Rock the JVM social network is up for registration! Launching when we have 10 users")
    barrier <- CyclicBarrier[IO](10)
    _ <- IO.dbg("[announcer] 1 to 14; 4 must wait")
    _ <- (1 toL 14).parTraverse(createUser(_, barrier)).start
    _ <- IO.sleep(4.seconds)
    _ <- IO.dbg("[announcer] 15 to 20; let in the waiting 4")
    _ <- (15 toL 20).parTraverse(createUser(_, barrier))
  } yield ()

  /**
   * Exercise: Implement your own CB with Ref + Deferred
   */

  def run: IO[Unit] = openNetwork()

  object my {
    trait CyclicBarrier[F[_]] {
      def await: F[Unit]
    }

    object CyclicBarrier {
      def apply[F[_] : Concurrent](n: Int): F[CyclicBarrier[F]] = for {
        state <- Concurrent[F].pure(Deferred[F, Unit].map(_ -> n))
        ref <- state.flatMap(Ref[F].of)
      } yield new CyclicBarrier {
        def await: F[Unit] =  state.flatMap { reset =>
          ref.flatModify {
            case (current, 1) => (reset, current.complete(()).void)
            case (current, n) => ((current, n - 1), current.get)
          }
        }
      }
    }
  }
}
