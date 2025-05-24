package rtj.polymorphic

import cats.Defer
import cats.effect.{IO, IOApp, MonadCancel, Resource, Sync}
import cats.syntax.all.*
import rtj.all.*

import java.io.{BufferedReader, InputStreamReader}

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

  /**
   *  Exercise - write a polymorphic console
   */
  trait Console[F[_]] {
    def println[A](a: A): F[Unit]
    def readLine(): F[String]
  }

  object Console {
    def apply[F[_]](using S: Sync[F]): F[Console[F]] = S.pure(System.in -> System.out).map { (in, out) =>
      new Console[F] {
        def println[A](a: A): F[Unit] = S.blocking(out.println(a))

        /**
         * There is a potential of problem hanging one if the threads from the blocking thread pool (or one of the CE threads)
         *
         * There is also `Sync.interruptible(true/false)`, which attempts to block the thread via thread interrupts in case of cancellation.
         * The flag indicates whether you want the threa interrupt signals to be sent repeatedly (true) or not (false).
         */
        def readLine(): F[String] = Resource
          .make(S.blocking { new BufferedReader(new InputStreamReader(in)) })(br => S.blocking { br.close() })
          .use { br => S.blocking { br.readLine() }  }
      }
    }
  }

  def demoConsole(): IO[Unit] = for {
    console <- Console[IO]
    _ <- console.println("Hi, what's your name?")
    name <- console.readLine()
    _ <- console.println(s"Hello $name, nice to meet you!")
  } yield ()

  def run: IO[Unit] = demoConsole()
}
