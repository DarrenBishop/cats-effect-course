package rtj.polymorphic

import cats.{Applicative, FlatMap, Functor, Monad}
import cats.effect.{Concurrent, Deferred, Fiber, IO, IOApp, MonadCancel, MonadCancelThrow, Outcome, OutcomeIO, Poll, Ref, Spawn}
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

  type RaceResultIO[A, B] = RaceResult[IO, A, B]

  def ourRacePair[A, B](ioa: IO[A], iob: IO[B]): IO[RaceResultIO[A, B]] = for {
    race <- IO.deferred[Either[OutcomeIO[A], OutcomeIO[B]]].onCancel(IO.void("Deferred canceled!"))
    fiba <- ioa.guaranteeCase(out => race.complete(Left(out)).void).onCancel(IO.void("IO-A canceled!")).start.onCancel(IO.void("IO-A-fiber canceled!"))
    fibb <- iob.guaranteeCase(out => race.complete(Right(out)).void).onCancel(IO.void("IO-B canceled!")).start.onCancel(IO.void("IO-B-fiber canceled!"))
    result <- race.get.onCancel(IO.void("IO-A vs IO-B race canceled!") >> (fiba.cancel, fibb.cancel).parTupled.void)
  } yield result match {
    case Left(outA) => Left(outA -> fibb)
    case Right(outB) => Right(fiba -> outB)
  }
  
  //def run: IO[Unit] = polymorphicAlarm[IO]
  def run: IO[Unit] = ourRacePair(IO.sleep(750.millis) >> IO("IO-A succeeded"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.void
  //def run: IO[Unit] = ourRacePair(IO.sleep(1000.millis) >> !?("IO-A errored"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.start >>= (fib => IO.sleep(250.millis) >> fib.cancel)
}
