package rtj.polymorphic

import cats.effect.{Concurrent, IO, IOApp, Temporal}
import cats.syntax.all.*
import cats.effect.syntax.all.*
import rtj.all.*

import scala.concurrent.duration.FiniteDuration

object PolymorphicTemporal extends IOApp.Simple {

  // Temporal - time-blocking effects

  object my {
    trait Temporal[F[_]] extends Concurrent[F] {
      def sleep(time: FiniteDuration): F[Unit] // semantically blocks this fiber for a specified time
    }

    // capabilities: pure, map/flatmap, raiseError, uncancelable, start, ref/deferred, + sleep

    val temporalIO = Temporal[IO] // given Temporal[IO] in scope
    val chainOIfEffects = IO.dbg("Loading...") >> IO.sleep(1.second) >> IO.dbg("Game ready!")
    val chainOfEffects_v2 = temporalIO.pure("Loading...").dbg >> temporalIO.sleep(1.second) >> temporalIO.pure("Game ready!").dbg
  }

  /**
   *    Exercise: generalize the following code
   */
  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] =
    IO.race(IO.sleep(duration).map(_ => !??("computation timed out!")), io).rethrow

  def demoTimeout() = timeout(IO("some operation").delayBy(600.millis), 1500.millis).dbg.silence


  def run: IO[Unit] = demoTimeout()
}
