package rtj.concurrency

import java.util.concurrent.{ExecutorService, Executors}

import scala.concurrent.ExecutionContext
import scala.util.Try

import cats.effect.{IO, IOApp}
import rtj.predef.*

object AsyncIOs extends IOApp.Simple {

  /**
    * IOs can run asynnchronously on fibers, without having to manually manage the fiber lifecylce
    */

  val threadPool: ExecutorService = Executors.newFixedThreadPool(8)
  given ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)

  type Call[A] = Either[Throwable, A]
  type Callback[A] = Call[A] => Unit

  def computeMeaningOfLife(): Int = {
    Thread.sleep(1000)
    println(s"[$threadName] computing the meaning of life on some other thread...")
    42
  }

  def computeMeaningOfLifeEither(): Either[Throwable, Int] =
    Try(computeMeaningOfLife()).toEither

  def computeMolOnThreadPool(): Unit =
    threadPool.execute(() => computeMeaningOfLifeEither())

  // lift a computation to an IO
  // async is foreign-function-interface (FFI)
  val asyncMolIO: IO[Int] = IO.async_ { cb => // CE thread blocks (semantically) until this cb is invoked (by some other thread)
    threadPool.execute { () => // computation not managed by CE
      val result = computeMeaningOfLifeEither()
      cb(result) // CE thread is notified with the result
    }
  }

  /**
    * Exercise 1: lift an async computation on ec to an IO.
    */
  def asyncToIO[A](computation: () => A)(using ec: ExecutionContext): IO[A] =
    IO.async_ { cb =>
      ec.execute { () =>
        cb {
          Try {
            val result = computation()
            println(s"[$threadName] computed $result")
            result
          }.toEither
        }
      }
    }

  val asyncMolIO_v2 = asyncToIO(computeMeaningOfLife)

  /**
    * Exercise 2: lift an async computation as a Future to IO.
    */
  lazy val molFuture: Future[Int] = Future(computeMeaningOfLife())

  //def run: IO[Unit] = asyncMolIO.dbg >> IO(threadPool.shutdown())
  //def run: IO[Unit] = asyncToIO(() => 42)(ec).dbg >> IO(threadPool.shutdown())
  def run: IO[Unit] = asyncMolIO_v2.dbg >> IO(threadPool.shutdown())
}
