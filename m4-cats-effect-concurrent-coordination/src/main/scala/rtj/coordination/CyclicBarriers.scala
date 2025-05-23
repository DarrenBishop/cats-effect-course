package rtj.coordination

import cats.effect.{Concurrent, IO, IOApp}
import cats.syntax.all.*
import rtj.all.{*, given}

import scala.util.Random

object CyclicBarriers extends IOApp.Simple {
  
  type CyclicBarrier[F[_]] = cats.effect.std.CyclicBarrier[F]
  val CyclicBarrier = cats.effect.std.CyclicBarrier

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
    _ <- (1 toL 20).parTraverse(createUser(_, barrier))
  } yield ()

  /**
   * Exercise: Implement your own CB with Ref + Deferred
   */

  def run: IO[Unit] = openNetwork()
}

object ny {
  trait CyclicBarrier[F[_]] {
    def await: F[Unit]
  }
  
  object CyclicBarrier {
    def apply[F[_] : Concurrent](count: Int): F[CyclicBarrier[F]] = ??? 
  }
}
