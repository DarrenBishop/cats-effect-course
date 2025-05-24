package rtj
package ce

import scala.concurrent.duration.FiniteDuration
import cats.{FlatMap, Foldable, Functor, MonadThrow}
import cats.effect.{IO, MonadCancel, Temporal}
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
    
    class Ops[A](fa: F[A]):
      def sleep(duration: FiniteDuration): F[A] = fa <* aux(duration)
      def sleep(millis: Long): F[A] = sleep(millis.millis)
      def pause(duration: FiniteDuration): F[A] = sleep(duration)
      def delay(duration: FiniteDuration): F[A] = aux(duration) >> fa
      def delay(millis: Long): F[A] = delay(millis.millis)

    inline def apply[A](fa: F[A]): Ops[A] = Ops(fa)

    extension [A](fa: F[A])
      private def ops = apply(fa)
      export ops.*
  }
  
  object Sleep {
    def apply[F[_]](using ev: Sleep[F]): Sleep[F] = ev

    given [F[_]] => (T: Temporal[F]) => Sleep[F]:
      override given FlatMap[F] = T
      protected def aux(duration: FiniteDuration): F[Unit] = T.sleep(duration)

    //given Sleep[IO]:
    //  given FlatMap[IO] = FlatMap[IO]
    //  protected def aux(duration: FiniteDuration): IO[Unit] = IO.sleep(duration)
  }

  trait Uncancelable[F[_]] { tc =>
    //extension [A](fa: F[A])
    //  def uncancelable: F[A]
    protected def aux[A](fa: F[A]): F[A]
    
    class Ops[A](fa: F[A]):
      def uncancelable: F[A] = aux(fa)

    inline def apply[A](fa: F[A]): Ops[A] = Ops(fa)

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

  trait Debug[F[_]] {
    given MonadThrow[F] = deferred
    def println(any: Any): F[Unit]
    
    class Ops[A](fa: F[A]):
      def dbg: F[A] = fa
        .flatTap(a => println(s"[$threadName] $a"))
        .handleErrorWith(ex => println(s"[$threadName] ${ex.name}(${ex.msg})") >> ex.raiseError)
      def dvoid: F[Unit] = dbg.void
      def silence: F[Unit] = fa.attempt.void

    inline def apply[A](fa: F[A]): Ops[A] = Ops(fa) 
    
    extension [A](fa: F[A])
      private inline def ops = apply(fa)
      export ops.*
  }
  
  object Debug {
    def apply[F[_]](using ev: Debug[F]): Debug[F] = ev
    
    given [F[_]] => (MonadThrow[F], Console[F]) => Debug[F]:
      override given MonadThrow[F] = MonadThrow[F]
      def println(any: Any): F[Unit] = Console[F].println(any)
    
    //given Debug[IO]:
    //  given MonadThrow[IO] = MonadThrow[IO]
    //  def println(any: Any): IO[Unit] = Console[IO].println(any)
  }

  //extension [F[_], A](fa: F[A])(using U: Uncancelable[F])
  //  //def uncancelable: F[A] = U.uncancelable(fa)
  //  private def u: U.Ops[A] = U(fa)
  //  export u.*
  //
  //extension [F[_], A](fa: F[A])(using S: Sleep[F])
  //  //def sleep(duration: FiniteDuration): F[A] = S.sleep(fa)(duration)
  //  //def sleep(millis: Long): F[A] = sleep(millis.millis)
  //  //def pause(duration: FiniteDuration): F[A] = sleep(duration)
  //  //def delay(duration: FiniteDuration): F[A] = S.delay(fa)(duration)
  //  //def delay(millis: Long): F[A] = delay(millis.millis)
  //  private def s: S.Ops[A] = S(fa)
  //  export s.*
  //
  ////extension [F[_], A](fa: F[A])(using M: MonadThrow[F], C: Console[F])
  ////  def dbg: F[A] = fa
  ////    .flatTap(a => C.println(s"[$threadName] $a"))
  ////    .handleErrorWith(ex => C.println(s"[$threadName] ${ex.name}(${ex.msg})") >> M.raiseError(ex))
  ////  def dvoid: F[Unit] = dbg.void
  ////  def silence: F[Unit] = fa.attempt.void
  //
  //extension [F[_], A](fa: F[A])(using D: Debug[F])
  //  private inline def d: D.Ops[A] = D(fa)
  //  export d.*

  extension [F[_], A](fa: F[A])(using U: Uncancelable[F], S: Sleep[F], D: Debug[F])
    private def u: U.Ops[A] = U(fa)
    export u.*
    private def s: S.Ops[A] = S(fa)
    export s.*
    private inline def d: D.Ops[A] = D(fa)
    export d.*
  
  extension [F[_]: Functor, C[_]: Foldable, A](fca: F[C[A]])
    def sum(using Numeric[A]): F[A] = fca.map(Foldable[C].foldLeft(_, Numeric[A].zero)(Numeric[A].plus))
  
  implicit def durationToSleep(d: FiniteDuration): IO[Unit] = IO.sleep(d)

  extension (ioo: IO.type)(using Sleep[IO], Debug[IO])
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
