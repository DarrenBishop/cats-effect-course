package rtj
package ce

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import cats.{Foldable, Functor, Monad, MonadThrow}
import cats.effect.IO
import cats.effect.std.Console
import cats.syntax.all.*


trait PkgSyntax {
  import rtj.syntax.*
  
  extension (ioo: IO.type)
    def dbg(any: => Any): IO[String] = IO(s"$any").dbg
    def void(any: => Any): IO[Unit] = IO.dbg(s"$any").void
    def pause(any: => Any, duration: FiniteDuration = 1.second): IO[Unit] = IO.void(s"$any").pause(duration)
    def err[A](msg: String): IO[A] = IO.raiseError[A](!??(msg))
    def pass[A](f: (IO[A] => IO[A]) => IO[A]): IO[A] = f(identity)

  def !? [A](msg: String): IO[A] = IO.err(msg)
  def canceled: IO[String] = IO("Fiber canceled")
  def !! : IO[Nothing] = canceled.dbg >>= !?

  //extension [A](io: IO[A])
  //  def dbg: IO[A] = io
  //    .flatTap(a => IO.println(s"[$threadName] $a"))
  //    .handleErrorWith(ex => IO.println(s"[$threadName] ${ex.name}(${ex.msg})") >> IO.raiseError(ex))
  //  def dvoid: IO[Unit] = dbg.void
  //  def delay(duration: FiniteDuration): IO[A] = IO.sleep(duration) >> io
  //  def delay(millis: Long): IO[A] = delay(millis.millis)
  //  def pause(duration: FiniteDuration): IO[A] = io <* IO.sleep(duration)
  //  def sleep(duration: FiniteDuration): IO[A] = pause(duration)
  //  def sleep(millis: Long): IO[A] = sleep(millis.millis)
  //  def silence: IO[Unit] = io.attempt.void

  trait Sleep[F[_]] {
    def sleep(duration: FiniteDuration): F[Unit]
  }
  
  object Sleep {
    given Sleep[IO] with {
      def sleep(duration: FiniteDuration) = IO.sleep(duration)
    }
  }

  extension [F[_], A](fa: F[A])(using S: Sleep[F], M: MonadThrow[F], C: Console[F])
    def dbg: F[A] = fa
      .flatTap(a => C.println(s"[$threadName] $a"))
      .handleErrorWith(ex => C.println(s"[$threadName] ${ex.name}(${ex.msg})") >> M.raiseError(ex))
    def dvoid: F[Unit] = dbg.void
    def delay(duration: FiniteDuration): F[A] = S.sleep(duration) >> fa
    def delay(millis: Long): F[A] = delay(millis.millis)
    def pause(duration: FiniteDuration): F[A] = fa <* S.sleep(duration)
    def sleep(duration: FiniteDuration): F[A] = pause(duration)
    def sleep(millis: Long): F[A] = sleep(millis.millis)
    def silence: F[Unit] = fa.attempt.void

  extension [F[_]: Functor, C[_]: Foldable, A](fca: F[C[A]])
    def sum(using Numeric[A]): F[A] = fca.map(Foldable[C].foldLeft(_, Numeric[A].zero)(Numeric[A].plus))
  
  implicit def durationToSleep(d: FiniteDuration): IO[Unit] = IO.sleep(d)
}

object syntax extends PkgSyntax

export syntax.*
