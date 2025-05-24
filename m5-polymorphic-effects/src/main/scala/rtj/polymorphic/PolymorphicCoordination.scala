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
  
  def run: IO[Unit] = polymorphicAlarm[IO]
}
