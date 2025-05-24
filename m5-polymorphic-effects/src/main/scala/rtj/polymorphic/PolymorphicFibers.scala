package rtj.polymorphic

import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.{Applicative, FlatMap, Functor, Monad}
import cats.effect.{Concurrent, Deferred, Fiber, IO, IOApp, MonadCancel, MonadCancelThrow, Outcome, Poll, Ref, Spawn}
import cats.syntax.all.*
import cats.effect.syntax.all.*
import rtj.all.*

object PolymorphicFibers extends IOApp.Simple {

  object my {
    trait GenSpawn[F[_], E] extends MonadCancel[F, E] {
      def start[A](fa: F[A]): F[Fiber[F, E, A]] // creates a fiber
      def never[A]: F[A] // a forever-suspending effect
      def cede: F[Unit] // a "yield" effect

      def racePair[A, B](fa: F[A], fb: F[B]): F[Either[
        (Outcome[F, E, A], Fiber[F, E, B]),
        (Fiber[F, E, A], Outcome[F, E, B])
      ]]
    }

    trait Spawn[F[_]] extends GenSpawn[F, Throwable]
  }
  
  val  mol = IO(42)
  val fiber: IO[Fiber[IO, Throwable, Int]] = mol.start
  
  // pure, map/flatMap, raiseError, uncancelable, start
  
  val spawnIO = Spawn[IO] // fetch the given/implicit Spawn[IO]
  
  def ioOnSomeThread[A](io: IO[A]): IO[Outcome[IO, Throwable, A]] = for {
    fib <- spawnIO.start(io) // io.start assumes the presence of Spawn[IO]
    result <- fib.join
  } yield result
  
  // generalize
  def effectOnSomeThread[F[_] : Spawn, A](fa: F[A]): F[Outcome[F, Throwable, A]] = for {
    fib <- fa.start
    result <- fib.join
  } yield result
  
  val molOnFiber = ioOnSomeThread(mol)
  val molOnFiber_v2 = effectOnSomeThread(mol)

  /**
   *    Exercise: generalize the following code
   */
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

  def run: IO[Unit] = molOnFiber_v2.dvoid
}
