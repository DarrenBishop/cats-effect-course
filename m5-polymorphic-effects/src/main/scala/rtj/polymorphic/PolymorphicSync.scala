package rtj.polymorphic

import cats.Defer
import cats.effect.{IO, IOApp, MonadCancel, Sync}

object PolymorphicSync extends IOApp.Simple {

  val aDelayedIO = IO.delay { // "suspend" computation in IO
    println("I'm an effect@!")
    42
  }

  val aBlockingIO = IO.blocking { // on some specific thread pool for blocking computations
    println("loading...")
    Thread.sleep(1000)
    42
  }

  // synchronous computation

  object my {
    trait Sync[F[_]] extends MonadCancel[F, Throwable] with Defer[F] {
      def delay[A](thunk: => A): F[A] // "suspension" of a computation - will run on the CE thread pool
      def blocking[A](thunk: => A): F[A] // runs on the blocking thread pool

      // defer comes for free
      def defer[A](fa: => F[A]): F[A] = flatten(delay(fa))
    }
  }

  val syncIO = Sync[IO] // given Sync[IO] in scope

  // capabilities: pure, map/flatMap, raiseError, uncancelable, + delay/blocking

  val aDelayedIO_v2 = syncIO.delay {
    println("I'm an effect@!")
    42
  } // same as IO.delay

  val aBlockingIO_v2 = syncIO.blocking {
    println("I'm an effect@!")
    Thread.sleep(1000)
    42
  } // same as IO.blocking

  val aDeferredIO = IO.defer(aDelayedIO)

  def run: IO[Unit] = ???
}
