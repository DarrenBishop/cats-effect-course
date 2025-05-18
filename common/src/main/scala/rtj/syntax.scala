package rtj

import cats.{FlatMap, Functor, Monoid}
import cats.effect.Sync
import cats.effect.std.Random
import cats.syntax.all.*

import scala.collection.immutable.NumericRange

trait PkgSyntax {
  export scala.concurrent.duration.DurationInt
  export scala.concurrent.duration.DurationLong
  export scala.concurrent.duration.DurationDouble

  def threadName: String = Thread.currentThread().getName

  extension (err: Throwable)
    def name: String = err.getClass.getName
    def msg: String = err.getMessage

  def !!![T]: T = ???
  def !??[T] (msg: String): Throwable = new RuntimeException(msg)
  def !!?[T] (msg: String): T = throw !??(msg)
  def ?? : Throwable =
    try { !!! }
    catch {
      case ex: Throwable => ex
    }

  // Random support
  //opaque type Seed = Int
  //object Seed:
  //  def apply(n: Int): Seed = n
  //  given Seed = 123456789
  case class Seed(s: Int)
  object Seed:
    given Seed(123456789)
  def seed(using S: Seed): Int = S.s
  type Rng[F[_]] = Random[F]
  def random[F[_]: Sync](using Seed): F[Rng[F]] = Random.scalaUtilRandomSeedInt(seed)
  def rng[F[_]: Rng]: Rng[F] = Random[F]
  
  // Option support
  def none[T]: Option[T] = None
  def some[T](value: T): Option[T] = Some(value)

  // Either support
  def left[L, R](value: L): Either[L, R] = Left(value)
  def right[L, R](value: R): Either[L, R] = Right(value)

  // List support
  def nil[E] = List.empty[E]

  extension [N](a: N)
    def toL(b: N)(using I: Integral[N]): List[N] = NumericRange.inclusive[N](a, b, I.one).toList
    def untilL(b: N)(using I: Integral[N]): List[N] = NumericRange[N](a, b, I.one).toList

  // Support for emptiness via Monoid
  def empty[T: Monoid]: T = Monoid[T].empty

  // Partial function support
  def partial[A, B](pf: PartialFunction[A, B]) = pf
  def ?> [A, B](pf: PartialFunction[A, B]) = partial(pf)
  
  // Given lambda support
  def givingly[G, R](g: G)(fu: G ?=> R): R = {
    given G = g;
    fu
  }
  
  extension [G](g: G)
    def give[R](fu: G ?=> R): R = givingly(g)(fu)
    def use[R](fu: G ?=> R): R = givingly(g)(fu)
  
  extension [F[_], A](fa: F[A])
    def gmap[B](f: A ?=> B)(using F: Functor[F]): F[B] = F.map(fa)(_.use(f))
    def mapU[B](f: A ?=> B)(using F: Functor[F]): F[B] = F.map(fa)(_.use(f))
    //inline def map[B](f: A ?=> B)(using Functor[F]): F[B] = mapU(f)
    def flatMapU[B](f: A ?=> F[B])(using F: FlatMap[F]): F[B] = F.flatMap(fa)(_.use(f))
    //inline def flatMap[B](f: A ?=> F[B])(using FlatMap[F]): F[B] = flatMapU(f)
}

object syntax extends PkgSyntax

