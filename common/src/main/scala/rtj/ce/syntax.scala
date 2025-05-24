package rtj
package ce

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import cats.{FlatMap, Foldable, Functor, Monad, MonadThrow}
import cats.effect.{IO, MonadCancel}
import cats.effect.std.Console
import cats.syntax.all.*

import scala.compiletime.deferred

trait PkgSyntax {
  import rtj.syntax.*

  trait Sleep[F[_]] { tc =>
    given FlatMap[F] = deferred
    protected def aux(duration: FiniteDuration): F[Unit]
    //extension [A](fa: F[A])
    //  def sleep(duration: FiniteDuration): F[A] = fa <* aux(duration)
    //  def sleep(millis: Long): F[A] = sleep(millis.millis)
    //  def pause(duration: FiniteDuration): F[A] = sleep(duration)
    //  def delay(duration: FiniteDuration): F[A] = aux(duration) >> fa
    //  def delay(millis: Long): F[A] = delay(millis.millis)

    case class Ops[A](fa: F[A]):
      def sleep(duration: FiniteDuration): F[A] = fa <* aux(duration)
      def sleep(millis: Long): F[A] = sleep(millis.millis)
      def pause(duration: FiniteDuration): F[A] = sleep(duration)
      def delay(duration: FiniteDuration): F[A] = aux(duration) >> fa
      def delay(millis: Long): F[A] = delay(millis.millis)

    inline def apply[A](fa: F[A]): Ops[A] = Ops[A](fa)

    extension [A](fa: F[A])
      private def ops = apply(fa)
      export ops.*
  }
  
  object Sleep {
    def apply[F[_]](using ev: Sleep[F]): Sleep[F] = ev
    given Sleep[IO]:
      given FlatMap[IO] = FlatMap[IO]
      protected def aux(duration: FiniteDuration): IO[Unit] = IO.sleep(duration)
  }

  trait Uncancelable[F[_]] { tc =>
    //extension [A](fa: F[A])
    //  def uncancelable: F[A]
    protected def aux[A](fa: F[A]): F[A]
    
    case class Ops[A](fa: F[A]):
      def uncancelable: F[A] = aux(fa)

    inline def apply[A](fa: F[A]): Ops[A] = Ops[A](fa)

    extension [A](fa: F[A])
      private inline def ops = apply(fa)
      export ops.*
  }

  object Uncancelable {
    def apply[F[_]](using ev: Uncancelable[F]): Uncancelable[F] = ev
    
    given [F[_], E] => (M: MonadCancel[F, E]) => Uncancelable[F]:
      def aux[A](fa: F[A]): F[A] = M.uncancelable(_ => fa)
      //extension [A](fa: F[A])
      //  def uncancelable: F[A] = M.uncancelable(_ => fa)
        
    //given Uncancelable[IO]:
    //  extension [A](ioa: IO[A])
    //    def uncancelable: IO[A] = IO.uncancelable(_ => ioa)
  }

  //trait Debug[F[_]] {
  //  extension [A](fa: F[A])
  //    def dbg: F[A]
  //    def dvoid: F[Unit]
  //    def silence: F[Unit]
  //}
  //object Debug {
  //  def apply[F[_]](using ev: Debug[F]): Debug[F] = ev
  //  given [F[_]] => (M: MonadThrow[F], C: Console[F]) => Debug[F]:
  //    extension [A](fa: F[A])
  //      def dbg: F[A] = fa
  //        .flatTap(a => C.println(s"[$threadName] $a"))
  //        .handleErrorWith(ex => C.println(s"[$threadName] ${ex.name}(${ex.msg})") >> M.raiseError(ex))
  //      def dvoid: F[Unit] = dbg.void
  //      def silence: F[Unit] = fa.attempt.void
  //      
  //  given Debug[IO] = apply
  //}

  extension [F[_], A](fa: F[A])(using U: Uncancelable[F])
    //def uncancelable: F[A] = U.uncancelable(fa)
    private def u: U.Ops[A] = U(fa)
    export u.*

  extension [F[_], A](fa: F[A])(using S: Sleep[F])
    //def sleep(duration: FiniteDuration): F[A] = S.sleep(fa)(duration)
    //def sleep(millis: Long): F[A] = sleep(millis.millis)
    //def pause(duration: FiniteDuration): F[A] = sleep(duration)
    //def delay(duration: FiniteDuration): F[A] = S.delay(fa)(duration)
    //def delay(millis: Long): F[A] = delay(millis.millis)
    private def s: S.Ops[A] = S(fa)
    export s.*

  extension [F[_], A](fa: F[A])(using M: MonadThrow[F], C: Console[F])
    def dbg: F[A] = fa
      .flatTap(a => C.println(s"[$threadName] $a"))
      .handleErrorWith(ex => C.println(s"[$threadName] ${ex.name}(${ex.msg})") >> M.raiseError(ex))
    def dvoid: F[Unit] = dbg.void
    def silence: F[Unit] = fa.attempt.void

  extension [F[_]: Functor, C[_]: Foldable, A](fca: F[C[A]])
    def sum(using Numeric[A]): F[A] = fca.map(Foldable[C].foldLeft(_, Numeric[A].zero)(Numeric[A].plus))
  
  implicit def durationToSleep(d: FiniteDuration): IO[Unit] = IO.sleep(d)

  extension (ioo: IO.type)(using Sleep[IO])
    def dbg(any: => Any): IO[String] = IO(s"$any").dbg
    def void(any: => Any): IO[Unit] = dbg(any).void
    def pause(any: => Any, duration: FiniteDuration = 1.second): IO[Unit] = void(any).pause(duration)
    def stall(any: => Any, duration: FiniteDuration = 1.second): IO[Unit] = void(any).delay(duration)
    def err[A](msg: String): IO[A] = IO.raiseError[A](!??(msg))
    def pass[A](f: (IO[A] => IO[A]) => IO[A]): IO[A] = f(identity)

  def !? [A](msg: String): IO[A] = IO.err(msg)
  def canceled: IO[String] = IO("Fiber canceled")
  def !! : IO[Nothing] = canceled.dbg >>= !?
}

object syntax extends PkgSyntax

export syntax.*
