package rtj.concurrency

import scala.concurrent.duration.FiniteDuration

import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{FiberIO, IO, IOApp, OutcomeIO}
import rtj.predef.*

object RacingIOs extends IOApp.Simple {

  def runWithSleep[A](value: A, duration: FiniteDuration): IO[A] = {
    IO.dbg(s"starting computation for $value") *>
      IO.sleep(duration) *>
    IO.dbg(s"completed computation for $value") *>
      IO(value)
  }.onCancel(IO.dbg(s"canceled computation for $value").void)

  def testRace() = {
    val meaningOfLife = runWithSleep(42, 1.second)
    val favLang = runWithSleep("Scala", 2.seconds)
    val first: IO[Either[Int, String]] = IO.race(meaningOfLife, favLang)

    /**
      * - both IOs run in separate threads
      * - the first one to finish will complete the result
      * - the loser will be canceled
      */

    first.flatMap {
      case Left(mol) => IO.dbg(s"Meaning of life won: $mol")
      case Right(lang) => IO.dbg(s"Favourite language won: $lang")
    }
  }

  def testRacePair() = {
    val meaningOfLife = runWithSleep(42, 1.second)
    val favLang = runWithSleep("Scala", 2.seconds)
    val raceResult: IO[Either[
      (OutcomeIO[Int], FiberIO[String]), // (winner outcome, loser fiber)
      (FiberIO[Int], OutcomeIO[String]) // (loser fiber, winner outcome)
    ]] = IO.racePair(meaningOfLife, favLang)

    /**
      * - both IOs run in separate threads
      */

    raceResult.flatMap {
      case Left((Succeeded(mol), fiber)) => fiber.cancel *> mol.flatMap(mol => IO.dbg(s"Meaning of life won: $mol; canceled lang"))
      case Right((fiber, Succeeded(lang))) => fiber.cancel *> lang.flatMap(lang => IO.dbg(s"Favourite language won: $lang; canceled meaning of life"))
    }
  }

  /**
    * Exercises:
    * 1 - implement a timeout pattern with race
    * 2 - a method to return a LOSING effect from a race (hint: use racePair)
    * 3 - implement race in terms of racePair
    */

  // 1
  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] =
    IO.race(IO.sleep(duration).map(_ => !??("computation timed out!")), io).rethrow

  val testTimeout_v1 = timeout(IO("some operation").delayBy(600.millis), 500.millis).dbg
  val testTimeout_v2 = IO("some operation").delayBy(600.millis).timeout(500.millis).dbg

  def testTimeout() = for {
    _ <- testTimeout_v1.silence
    _ <- testTimeout_v2.silence
  } yield ()

  // 2
  def unrace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
    IO.racePair(ioa, iob).flatMap {
      case Left((_, fiber)) => fiber.join.flatMap {
        case Succeeded(loser) => loser.map(Right(_))
        case Errored(err) => IO.raiseError(err)
        case Canceled() => !!
      }
      case Right((fiber, _)) => fiber.join.flatMap {
        case Succeeded(loser) => loser.map(Left(_))
        case Errored(err) => IO.raiseError(err)
        case Canceled() => !!
      }
    }

  def testUnrace() = for {
    _ <- unrace(IO.dbg("first operation: fast"), IO.dbg("second operation: slow").andWait(500.millis)).dbg.silence
    _ <- unrace(IO.dbg("first operation: slow").andWait(500.millis), IO.dbg("second operation: fast")).dbg.silence
  } yield ()

  // 3
  def simpleRace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
    IO.racePair(ioa, iob).flatMap {
      case Left((Succeeded(ioa), fiber)) => fiber.cancel *> ioa.map(Left(_))
      case Left((Errored(err), fiber)) => fiber.cancel *> IO.raiseError(err)
      case Left((Canceled(), fiber)) => fiber.join.flatMap {
        case Succeeded(loser) => loser.map(Right(_))
        case Errored(err) => IO.raiseError(err)
        case Canceled() => !?("both canceled")
      }
      case Right((fiber, Succeeded(iob))) => fiber.cancel *> iob.map(Right(_))
      case Right((fiber, Errored(err))) => fiber.cancel *> IO.raiseError(err)
      case Right((fiber, Canceled())) => fiber.join.flatMap {
        case Succeeded(loser) => loser.map(Left(_))
        case Errored(err) => IO.raiseError(err)
        case Canceled() => !?("both canceled")
      }
    }

  def testSimpleRace() = for {
    //_ <- simpleRace(IO.dbg("first slow").delayBy(100.millis), IO.dbg("second success")).dbg.void

    //_ <- simpleRace(!?("first failure").dbg, IO.dbg("second success").delayBy(100.millis)).dbg.void
    //_ <- simpleRace(!?("first failure").dbg, !?("second failure").dbg.delayBy(100.millis)).dbg.silence
    //_ <- simpleRace(!?("first failure").dbg, IO.dbg("second canceled") *> IO.canceled.delayBy(100.millis)).dbg.silence

    //_ <- simpleRace(IO.dbg("first canceled") *> IO.canceled, IO.dbg("second success").delayBy(100.millis)).dbg.void
    //_ <- simpleRace(IO.dbg("first canceled") *> IO.canceled, !?("second failure").dbg.delayBy(100.millis)).dbg.silence
    //_ <- simpleRace(IO.dbg("first canceled") *> IO.canceled, IO.dbg("second canceled") *> IO.canceled.delayBy(100.millis)).dbg.silence

    //_ <- simpleRace(IO.sleep(100.millis).guarantee(IO.dbg("first canceled").void), IO.dbg("second operation")).dbg.void

    _ <- simpleRace(IO.dbg("first success").delayBy(100.millis), !?("second failure").dbg).dbg.silence
    _ <- simpleRace(!?("fist failure").dbg.delayBy(100.millis), !?("second failure").dbg).dbg.silence
    _ <- simpleRace(IO.dbg("first canceled") *> IO.canceled.delayBy(100.millis), !?("second failure").dbg).dbg.silence

    //_ <- simpleRace(IO.dbg("first success").delayBy(100.millis), IO.dbg("second canceled") *> IO.canceled).dbg.void
    //_ <- simpleRace(!?("first failure").dbg.delayBy(100.millis), IO.dbg("second canceled") *> IO.canceled).dbg.silence
    //_ <- simpleRace(IO.dbg("first canceled") *> IO.canceled.delayBy(100.millis), IO.dbg("second canceled") *> IO.canceled).dbg.silence

  } yield ()

  //def run: IO[Unit] = testTimeout()
  //def run: IO[Unit] = testUnrace()
  def run: IO[Unit] = testSimpleRace()
}
