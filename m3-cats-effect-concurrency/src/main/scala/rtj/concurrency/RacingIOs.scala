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
  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] = ???

  // 2
  def unrace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] = ???

  // 3
  def simpleRace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] = ???

  def run: IO[Unit] = testRacePair().void
}
