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
  def generalRace[F[_], A, B](fa: F[A], fb: F[B])(using S: Spawn[F]): F[Either[A, B]] =
    S.racePair(fa, fb).flatMap {
      case Left((Succeeded(winner), fiber)) => fiber.cancel >> winner.map(Left(_))
      case Left((Errored(err), fiber)) => fiber.cancel >> S.raiseError(err)
      case Left((Canceled(), fiber)) => fiber.join.flatMap {
        case Succeeded(loser) => loser.map(Right(_))
        case Errored(err) => S.raiseError(err)
        case Canceled() => S.raiseError(!??("both canceled"))
      }
      case Right((fiber, Succeeded(winner))) => fiber.cancel >> winner.map(Right(_))
      case Right((fiber, Errored(err))) => fiber.cancel >> S.raiseError(err)
      case Right((fiber, Canceled())) => fiber.join.flatMap {
        case Succeeded(loser) => loser.map(Left(_))
        case Errored(err) => S.raiseError(err)
        case Canceled() => S.raiseError(!??("both canceled"))
      }
    }

  def testGeneralRace() = for {
    _ <- generalRace(IO.dbg("first slow").delayBy(100.millis), IO.dbg("second success")).dbg.void

    //_ <- generalRace(!?("first failure").dbg, IO.dbg("second success").delayBy(100.millis)).dbg.void
    //_ <- generalRace(!?("first failure").dbg, !?("second failure").dbg.delayBy(100.millis)).dbg.silence
    //_ <- generalRace(!?("first failure").dbg, IO.dbg("second canceled") *> IO.canceled.delayBy(100.millis)).dbg.silence

    //_ <- generalRace(IO.dbg("first canceled") *> IO.canceled, IO.dbg("second success").delayBy(100.millis)).dbg.void
    //_ <- generalRace(IO.dbg("first canceled") *> IO.canceled, !?("second failure").dbg.delayBy(100.millis)).dbg.silence
    //_ <- generalRace(IO.dbg("first canceled") *> IO.canceled, IO.dbg("second canceled") *> IO.canceled.delayBy(100.millis)).dbg.silence

    //_ <- generalRace(IO.sleep(100.millis).guarantee(IO.dbg("first canceled").void), IO.dbg("second operation")).dbg.void

    _ <- generalRace(IO.dbg("first slow success").delayBy(100.millis), !?("second failure").dbg).dbg.silence
    _ <- generalRace(!?("first slow failure").dbg.delayBy(100.millis), !?("second failure").dbg).dbg.silence
    _ <- generalRace(IO.dbg("first slow canceled").delayBy(100.millis) *> IO.canceled, !?("second failure").dbg).dbg.silence

    //_ <- generalRace(IO.dbg("first success").delayBy(100.millis), IO.dbg("second canceled") *> IO.canceled).dbg.void
    //_ <- generalRace(!?("first failure").dbg.delayBy(100.millis), IO.dbg("second canceled") *> IO.canceled).dbg.silence
    //_ <- generalRace(IO.dbg("first canceled") *> IO.canceled.delayBy(100.millis), IO.dbg("second canceled") *> IO.canceled).dbg.silence

  } yield ()

  //def run: IO[Unit] = molOnFiber_v2.dvoid
  def run: IO[Unit] = testGeneralRace()
}
