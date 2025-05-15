package rtj

import cats.Monoid

trait PkgSyntax {
  export scala.concurrent.duration.DurationInt
  export scala.concurrent.duration.DurationLong
  export scala.concurrent.duration.DurationDouble

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

  // Option support
  def none[T]: Option[T] = None
  def some[T](value: T): Option[T] = Some(value)

  // Either support
  def left[L, R](value: L): Either[L, R] = Left(value)
  def right[L, R](value: R): Either[L, R] = Right(value)

  // List support
  def nil[E] = List.empty[E]

  // Support for emptiness via Monoid
  def empty[T: Monoid]: T = Monoid[T].empty

  // Partial function support
  def partial[A, B](pf: PartialFunction[A, B]) = pf
  def ?> [A, B](pf: PartialFunction[A, B]) = partial(pf)
}

object syntax extends PkgSyntax

