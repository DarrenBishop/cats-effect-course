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
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)

  type Call[A] = Either[Throwable, A]
  type Callback[A] = Call[A] => Unit

  def computeMeaningOfLife(): Either[Throwable, Int] = Try {
    Thread.sleep(1000)
    println(s"[$threadName] computing the meaning of life on some other thread...")
    42
  }.toEither

  def computeMolOnThreadPool(): Unit =
    threadPool.execute(() => computeMeaningOfLife())

  // lift a computation to an IO
  // async is foreign-function-interface (FFI)
  val asyncMolIO: IO[Int] = IO.async_ { cb => // CE thread blocks (semantically) until this cb is invoked (by some other thread)
    threadPool.execute { () => // computation not managed by CE
      val result = computeMeaningOfLife()
      cb(result) // CE thread is notified with the result
    }
  }

  /**
    * Exercise
    */
  def asyncToIO[A](computation: () => A)(ec: ExecutionContext): IO[A] = ???

  def run: IO[Unit] = asyncMolIO.dbg >> IO(threadPool.shutdown())
}
