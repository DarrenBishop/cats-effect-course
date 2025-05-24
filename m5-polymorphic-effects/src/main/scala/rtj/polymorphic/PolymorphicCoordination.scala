package rtj.polymorphic

import cats.{Applicative, FlatMap, Functor, Monad}
import cats.effect.{Concurrent, Deferred, Fiber, IO, IOApp, MonadCancel, MonadCancelThrow, Outcome, Poll, Ref, Spawn}
import cats.syntax.all.*
import cats.effect.syntax.all.*
import rtj.all.*

object PolymorphicCoordination extends IOApp.Simple {

  // Concurrent - Ref + Deferred for ANY effect type
  object my {
    trait Concurrent[F[_]] extends Spawn[F] {
      def ref[A](a: A): F[Ref[F, A]]
      def deferred[A]: F[Deferred[F, A]]
    }
  }

  val concurrentIO = Concurrent[IO] // given instance if Concurrent[IO]

  val aRef = Ref[IO].of(42) // given/implicit Concurrent[IO] in scope
  val aDeferred = Deferred[IO, Int] // given/implicit Concurrent[IO] in scope

  val aRef_v2 = concurrentIO.ref(42)
  val aDeferred_v2 = concurrentIO.deferred[Int]

  // capabilities: pure, map/flatMap, raiseError, uncancelable, stat (fibers), + ref/deferred

  def polymorphicAlarm[F[_]: {Debug, Sleep}](using C: Concurrent[F]): F[Unit] = {
    def tick(time: Ref[F, Int], signal: Deferred[F, Int]): F[Unit] = for {
      _ <- ().pure.sleep(1.second)
      nt <- time.updateAndGet(_ + 1)
      _ <- s"the time is $nt".pure.dbg
      _ <- if (nt == 10) then signal.complete(nt).void else tick(time, signal)
    } yield ()

    def notification(signal: Deferred[F, Int]): F[Unit] =
      "timer started on some other fiber".pure.dbg >> signal.get >> "time's up!".pure.dvoid

    for {
      time <- C.ref(0)
      signal <- C.deferred[Int]
      _ <- notification(signal).start
      _ <- tick(time, signal)
    } yield ()
  }

  /**
   *    Exercise:
   *    1. Generalize racePair
   *    2. Generalize the Mutex primitive for any F
   */

  type RaceResult[F[_], A, B] = Either[
    (Outcome[F, Throwable, A], Fiber[F, Throwable, B]), // (winner outcome, loser fiber)
    (Fiber[F, Throwable, A], Outcome[F, Throwable, B]) // (loser fiber, winner outcome)
  ]

  def generalRacePair[F[_]: Debug, A, B](fa: F[A], fb: F[B])(using C: Concurrent[F]): F[RaceResult[F, A, B]] = for {
    race <- C.deferred[Either[Outcome[F, Throwable, A], Outcome[F, Throwable, B]]].onCancel("Deferred canceled!".pure.dvoid)
    fiba <- fa.guaranteeCase(out => race.complete(Left(out)).void).onCancel("IO-A canceled!".pure.dvoid).start.onCancel("IO-A-fiber canceled!".pure.dvoid)
    fibb <- fb.guaranteeCase(out => race.complete(Right(out)).void).onCancel("IO-B canceled!".pure.dvoid).start.onCancel("IO-B-fiber canceled!".pure.dvoid)
    result <- race.get.onCancel("IO-A vs IO-B race canceled!".pure.dvoid >> fiba.cancel.start >> fibb.cancel.start.void)
  } yield result match {
    case Left(outA) => Left(outA -> fibb)
    case Right(outB) => Right(fiba -> outB)
  }
  
  //def run: IO[Unit] = polymorphicAlarm[IO]
  def run: IO[Unit] = generalRacePair(IO.sleep(750.millis) >> IO("IO-A succeeded"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.void
  //def run: IO[Unit] = generalRacePair(IO.sleep(1000.millis) >> !?("IO-A errored"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.start >>= (fib => IO.sleep(250.millis) >> fib.cancel)
}
