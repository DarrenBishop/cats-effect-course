package rtj.polymorphic

import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.{Applicative, FlatMap, Functor, Monad}
import cats.effect.{Concurrent, Deferred, IO, IOApp, MonadCancelThrow, Poll, Ref, MonadCancel as CEMonadCancel}
import cats.syntax.all.*
import cats.effect.syntax.all.*
import rtj.all.*

object PolymorphicCancellation extends IOApp.Simple {

  object my {
    trait ApplicativeError[F[_], E] extends Applicative[F] {
      def raiseError[A](error: E): F[A]
      def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]
      //extension [A](fa: F[A])
      //  def handleErrorWith(f: E => F[A]): F[A]
    }

    trait MonadError[F[_], E] extends ApplicativeError[F, E],  Monad[F]

    // MonadCancel

    trait MonadCancel[F[_], E] extends MonadError[F, E] {
      def canceled: F[Unit]
      def uncancelable[A](f: Poll[F] => F[A]): F[A]
    }
  }

  // monadCancel for IO
  val monadCancelIO: CEMonadCancel[IO, Throwable] = CEMonadCancel[IO, Throwable]

  // we can create values
  val molIO: IO[Int] = monadCancelIO.pure(42)
  val ambitiousMolIO: IO[Int] = monadCancelIO.map(molIO)(_ * 10)

  val mustCompute = monadCancelIO.uncancelable { _ =>
    for {
      _ <- monadCancelIO.pure("once started, I can't go back...")
      res <- monadCancelIO.pure(56)
    } yield res
  }

  def mustComputeGeneral[F[_], E](using mc: CEMonadCancel[F, E]): F[Int] = mc.uncancelable { _ =>
    for {
      _ <- mc.pure("once started, I can't go back...")
      res <- mc.pure(56)
    } yield res
  }

  val mustCompute_v2 = mustComputeGeneral[IO, Throwable]

  // allow cancellation listeners
  val mustComputeWithListener = mustCompute.onCancel(IO.void("I'm being cancelled!"))
  val mustComputeWithListener_v2 = monadCancelIO.onCancel(mustCompute, IO.void("I'm being cancelled!")) // same
  // onCancel as an extension method

  // allow finalizers: guarantee, guaranteeCase
  val aComputationWithFinalizers = monadCancelIO. guaranteeCase(IO(42)) {
    case Succeeded(fa) => fa.flatMap(a => IO.void(s"succeeded: $a"))
    case Errored(err) => IO.void(s"errored: $err")
    case Canceled() => IO.void("canceled")
  }

  // bracket pattern is specific to MonadCancel
  val aComputationWithBracket = monadCancelIO.bracket {
    IO.dbg("Acquire the meaning of life").as(42)
  } {
    value => IO.void(s"Using the meaning of life: $value")
  } {
    _ => IO.void("Releasing the meaning of life")
  }

  /**
   *    Exercise: generalize a piece of code
   */
  def inputPassword[F[_]: {Debug, Monad, Sleep}]: F[String] =
    "Input password".pure.dbg >>
      "(typing password)".pure.dbg.sleep(2000.millis) >>
      "RockTheJVM123!".pure

  def verifyPassword[F[_]: {Applicative, Debug, Sleep}]: String => F[Boolean] = { password =>
    "verifying...".pure.dbg.sleep(2000.millis).as(password == "RockTheJVM123!")
  }

  def authFlow[F[_]: {Debug, Sleep}](using M: MonadCancelThrow[F]): F[Unit] = M.uncancelable { (poll: Poll[F]) =>
    for {
      pw <- poll(inputPassword).onCancel("Authentication timed out. Try again later!".pure.dvoid) // this is cancelable
      verified <- verifyPassword(pw) // this is NOT cancelable
      _ <- (if (verified) "Authentication successful." else "Authentication failed!").pure.dbg // this is NOT cancelable
    } yield ()
  }

  def authProgram[F[_]: {Concurrent, Debug, Sleep}] = for {
    authFib <- authFlow.start
    _ <- ().pure.sleep(3.seconds) >> "Authentication timeout, attempting cancel...".pure.dbg >> authFib.cancel
    _ <- authFib.join
  } yield ()

  //def run: IO[Unit] = aComputationWithBracket
  def run: IO[Unit] = authProgram[IO] >> IO("abc").sleep(1.seconds).dbg.void
}
