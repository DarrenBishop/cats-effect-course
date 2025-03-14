package rtj.concurrency

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

  //def run: IO[Unit] = sameThreadIOs()
  //def run: IO[Unit] = differentThreadIOs()
  //def run: IO[Unit] =
    //runOnSomeOtherThread(meaningOfLife) // IO(Succeeded(IO(42)))
    //.dbg.void
  //def run: IO[Unit] = throwOnAnotherThread().dbg.void
  def run: IO[Unit] = testCancel().dbg.void
}
