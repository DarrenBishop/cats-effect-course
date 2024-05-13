package rtj

import cats.Monoid


def !!![T]: T = ???

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

export ec.Syntax.*
