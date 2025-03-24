package rtj.concurrency

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.{IO, IOApp}
import rtj.predef.*

object BlockingIOs extends IOApp.Simple {

  val someSleeps = for {
    _ <- IO.sleep(1.second).dbg // semantic blocking
    _ <- IO.sleep(2.second).dbg
  } yield ()

  // really blocking IOs
  val aBlockingIO = IO.blocking {
    Thread.sleep(1000)
    println(s"[$threadName]: computed a blocking code")
  } // will evaluate on a thread from ANOTHER thread-pool specific for blocking calls

  // yielding
  val iosOnManyThreads = for {
    _ <- IO.dbg("first")
    _ <- IO.cede // a signal to yield control over the thread - equivalnet to CE2 IO.shift
    _ <- IO.dbg("second")
    _ <- IO.cede
    _ <- IO.dbg("third")
  } yield ()

  def testThousandEffectsSwitch = {
    val threadPool = Executors.newFixedThreadPool(8)
    val ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)
    (1 to 1000).map(IO.dbg(_)).reduce(_ >> IO.cede >> _).evalOn(ec).guarantee(IO(threadPool.shutdown()))
  }

  /**
    * IO.blocking & IO.sleep calls yield (cede) control over the calling thread automatically
    */

  //def run: IO[Unit] = someSleeps
  //def run: IO[Unit] = iosOnManyThreads
  def run: IO[Unit] = testThousandEffectsSwitch.void
}
