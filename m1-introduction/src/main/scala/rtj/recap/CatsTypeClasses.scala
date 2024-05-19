package rtj.recap

object CatsTypeClasses {

  /*
    Cats Type Classes
    - applicative
    - functor
    - flatMap
    - monad
    - apply
    - applicativeError/monadError
    - traverse
   */

  // Functor - "mappable" data structures
  trait MyFunctor[F[_]]:
    def map[A, B](initialValue: F[A])(f: A => B): F[B]

  import cats.Functor
  //import cats.instances.list.*
  val listFunctor = Functor[List]

  // generalizable "mapping" APIs
  def increment[F[_]](container: F[Int])(using F: Functor[F]): F[Int] =
    F.map(container)(_ + 1)

  import cats.syntax.functor.* // import the `map` extension method
  def increment_v2[F[_]: Functor](container: F[Int]): F[Int] =
    container.map(_ + 1)

  // Applicative - the ability to "wrap" or "lift" values in the context
  trait MyApplicative[F[_]] extends MyFunctor[F]:
    def pure[A](value: A): F[A]

  import cats.Applicative
  val applicativeList = Applicative[List]
  val aSimpleList: List[Int] = applicativeList.pure(43)
  import cats.syntax.applicative.* // import the `pure` extension method
  val aSimpleList_v2: List[Int] = 43.pure[List]

  // FlatMap - the ability to chain multiple wrapper (contextual) computations
  trait MyFlatMap[F[_]] extends MyFunctor[F]:
    def flatMap[A, B](container: F[A])(f: A => F[B]): F[B]

  import cats.FlatMap
  val flatMapList = FlatMap[List]
  import cats.syntax.flatMap.* // import the `flatMap` extension method
  def crossProduct[F[_]: FlatMap, A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    fa.flatMap(a => fb.map(b => (a, b)))

  // Monad - Applicative + FlatMap
  trait MyMonad[F[_]] extends MyApplicative[F] with MyFlatMap[F]:
    def map[A, B](initialValue: F[A])(f: A => B): F[B] =
      flatMap(initialValue)(a => pure(f(a)))

  import cats.Monad
  val monadList = Monad[List]
  def crossProduct_v2[F[_]: Monad, A, B](fa: F[A], fb: F[B]): F[(A, B)] = for
    a <- fa
    b <- fb
  yield (a, b)

  /*
        Functor ->   FlatMap   ->
                  \              \
                   -> Applicative -> Monad
   */

  // Error-handling type classes
  // ApplivativeError - Applicative + Error Handling
  trait MyApplicativeError[F[_], E] extends MyApplicative[F]:
    def raiseError[A](e: E): F[A]

  import cats.ApplicativeError
  type ErrorOr[A] = Either[String, A]
  val appErrorEither = ApplicativeError[ErrorOr, String]
  val desiredValue: ErrorOr[Int] = appErrorEither.pure(42)
  val failedValue: ErrorOr[Int] = appErrorEither.raiseError("Something failed")
  import cats.syntax.applicativeError.* // import the `raiseError` extension method
  val failedValue_v2: ErrorOr[Int] = "Something failed".raiseError[ErrorOr, Int]

  // MonadError - Monad + Error Handling
  trait MyMonadError[F[_], E] extends MyApplicativeError[F, E] with MyMonad[F]
  import cats.MonadError
  val monadErrorEither = MonadError[ErrorOr, String]

  // Traverse - the ability to traverse a data structure and apply a function
  trait MyTraverse[F[_]]:
    def traverse[G[_], A, B](fa: F[A])(f: A => G[B]): G[F[B]] = ???

  // turn nested wrappers inside out
  val listOfOptions: List[Option[Int]] = List(Some(1), Some(2), Some(43))
  import cats.Traverse
  val listTraverse = Traverse[List]
  val optionList: Option[List[Int]] = listTraverse.traverse(List(1, 2, 3))(Option(_))
  import cats.syntax.traverse.* // import the `traverse` extension method
  val optionList_v2: Option[List[Int]] = List(1, 2, 3).traverse(Option(_))

  def main(args: Array[String]): Unit = {

    println(increment(List(1, 2, 3)))
    println(increment_v2(List(1, 2, 3)))
    println(crossProduct(List(1, 2), List("a", "b")))
    println(crossProduct_v2(List(1, 2), List("a", "b")))
    println(desiredValue)
    println(failedValue)
    println(failedValue_v2)
    println(optionList)
    println(optionList_v2)
  }
}
