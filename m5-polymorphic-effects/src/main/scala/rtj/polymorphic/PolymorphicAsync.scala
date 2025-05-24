package rtj.polymorphic

import cats.effect.{Async, Concurrent, IO, IOApp, MonadCancel, Resource, Sync, Temporal}
import cats.syntax.all.*
import rtj.all.*

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object PolymorphicAsync extends IOApp.Simple {

  object my {
    // Asynchronous computations, "suspended" in F
    trait Async[F[_]] extends Sync[F] with Temporal[F] {
      // fundamental description of async computation
      def executionContext: F[ExecutionContext]
      def async[A](cb: (Either[Throwable, A] => Unit) => F[Option[F[Unit]]]): F[A]
      def evalOn[A](fa: F[A], ec: ExecutionContext): F[A]

      def async_[A](cb: (Either[Throwable, A] => Unit) => Unit): F[A] =
        async { cb_ => as(delay(cb(cb_)), None) }
      // never-ending effect
      def never[A]: F[A] = async(_ => pure(None))
    }
  }

  val asyncIO = Async[IO] // given Async[IO] in scope

  // capabilities: pure, map/flatMap, raiseError, uncancelable, start, ref/deferred, sleep, delay/blocking, +
  val ec = asyncIO.executionContext

  // power: async_ + async: FFI
  val threadPool = Executors.newFixedThreadPool(10)

  type Callback[A] = Either[Throwable, A] => Unit

  val asyncMeaningOfLife = IO.async_ { (cb: Callback[Int]) =>
    // start computation on some other thread pool
    threadPool.execute { () =>
      println(s"[${threadName}] Computing an aync MOL")
      cb(Right(42))
    }
  }

  val asyncMeaningOfLife_v2 = asyncIO.async_ { (cb: Callback[Int]) =>
    // start computation on some other thread pool
    threadPool.execute { () =>
      println(s"[${threadName}] Computing an aync MOL")
      cb(Right(42))
    }
  } // same

  val asyncMeaningOfLifeComplex = IO.async { (cb: Callback[Int]) =>
    IO {
      // start computation on some other thread pool
      threadPool.execute { () =>
        println(s"[${threadName}] Computing an async MOL")
        cb(Right(42))
      }
    }.as(Some(IO("Canceled!").dvoid)) // <-- finalizer in case the computation gets canceled
  }

  val asyncMeaningOfLifeComplex_v2 = asyncIO.async { (cb: Callback[Int]) =>
    IO {
      // start computation on some other thread pool
      threadPool.execute { () =>
        println(s"[${threadName}] Computing an async MOL")
        cb(Right(42))
      }
    }.as(Some(IO("Canceled!").dvoid)) // <-- finalizer in case the computation gets canceled
  } // same

  val myExecutionContext = ExecutionContext.fromExecutorService(threadPool)
  val asyncMeaningOfLife_v3 = asyncIO.evalOn(IO.dbg(42), myExecutionContext).guarantee((IO(threadPool.shutdown())))

  // never
  val neverIO = asyncIO.never

  /**
   *  Exercises
   *    1 - implement never and async_ in terms of the big async.
   *    2 - tuple two effects with different requirements
   */

  def firstEffect[F[_]: Concurrent, A](a: A): F[A] = Concurrent[F].pure(a)
  def secondEffect[F[_]: Sync, A](a: A): F[A] = Sync[F].pure(a)

  def tupledEffect[F[_]: Async, A](a: A): F[(A, A)] = for {
    fe <- firstEffect(a)
    se <- secondEffect(a)
  } yield (fe, se)

  def run: IO[Unit] = tupledEffect[IO, String]("I love Scala!").dvoid
}
