package rtj.concurrency

import scala.concurrent.duration.FiniteDuration

import rtj.predef.*
import cats.effect.{Fiber, FiberIO, IO, IOApp, Outcome}
import cats.effect.Outcome.*
import cats.syntax.all.*

object Fibers extends IOApp.Simple {

  val meaningOfLife = IO.pure(42)
  val favLang = "Scala".pure[IO]

  def sameThreadIOs(): IO[Unit] = for {
    _ <- meaningOfLife.dbg
    _ <- favLang.dbg
  } yield ()

  // introduce the Fiber
  def createFiber: Fiber[IO, Throwable, String] = ??? // almost impossible to create fibers manually

  // the fiber is not actually started, but the fiber allocation is wrapped in another effect
  val aFiber: IO[FiberIO[Int]] = meaningOfLife.dbg.start

  def differentThreadIOs() = for {
    _  <-aFiber
    _ <- favLang.dbg
  } yield ()

  // joining a fiber

  def runOnSomeOtherThread[A](io: IO[A]): IO[Outcome[IO, Throwable, A]] = for {
    fib <- io.start
    result <- fib.join // an effect, which semantically blocks i.e. waits for the fiber to complete
  } yield result
  /*
    possible outcomes:
    - success with an IO
    - failure with an exception
    - cancelled
   */

  val someIOOnAnotherThread = runOnSomeOtherThread((meaningOfLife))
  val someResultFromAnotherThread = someIOOnAnotherThread.flatMap {
    case Succeeded(effect) => effect
    case Errored(ex) => IO(0)
    case Canceled() => IO(0)
  }

  def throwOnAnotherThread() = for {
    fib <- IO.raiseError[Int](new RuntimeException("no number for you")).start
    result <- fib.join
  } yield result

  def testCancel() = {
    val task = IO("starting").dbg >> IO.sleep(1.second) >> IO("done").dbg
    // onCancel is a "finalizer", allowing you to free up resources in case you get cancelled
    val taskWithCancellationHandler = task.onCancel(IO("i'm being cancelled!").dbg.void)

    for {
      fib <- taskWithCancellationHandler.start // on a separate thread
      _ <- IO.sleep(500.millis) >> IO("cancelling").dbg // running on the calling thead
      _ <- fib.cancel // ... calling thead
      result <- fib.join // ... calling thead
    } yield result
  }

/**
 * Exercises:
 *
 *   1. Write a function that runs an IO on another thread, and, depending on the result of the fiber
 *     - return the result in an IO
 *     - if errored or cancelled, return a failed IO
 *
 *   2. Write a function that takes two IOs, runs them on different fibers and returns an IO with a tuple containing both result
 *     - if both Is complete successfully, tuple their results
 *     - if the first I0 returns an error, raise that error (ignoring the second IO's result/error)
 *     - if the first I0 doesn't error but second I0 returns an error, raise that error
 *     - if one (or both) canceled, raise a RuntimeException
 *
 *   3. Write a function that adds a timeout to an I0:
 *     - I0 runs on a fiber
 *     - if the timeout duration passes, then the fiber is canceled
 *     - the method returns an IO[A] which contains
 *       - the original value if the computation is successful before the timeout signal
 *       - the exception if the computation is failed before the timeout signal
 *       - a RuntimeException if it times out (i.e. cancelled by the timeout)
 */

// 1
def processResultsFromFiber[A](io: IO[A]): IO[A] = ???

// 2
def tupleIOs[A, B](ioa: IO[A], iob: IO[B]): IO[(A, B)] = ???

// 3
def timeout[A](io: IO[A], timeout: FiniteDuration): IO[A] = ???


def runExercises(): IO[Unit] = for {
  answer1 <- processResultsFromFiber(IO("Exercise 1"))
  answer2 <- tupleIOs(IO("Exercise 2").dbg, IO(2222).delay(1000))
  answer3 <- processResultsFromFiber(IO("Exercise 3"))
} yield ()

  //def run: IO[Unit] = sameThreadIOs()
  //def run: IO[Unit] = differentThreadIOs()
  //def run: IO[Unit] =
  //runOnSomeOtherThread(meaningOfLife) // IO(Succeeded(IO(42)))
  //.dbg.void
  //def run: IO[Unit] = throwOnAnotherThread().dbg.void
  //def run: IO[Unit] = testCancel().dbg.void

def run: IO[Unit] = runExercises()
}
