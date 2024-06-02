package rtj.effectsio

import scala.util.Try
import cats.effect.IO
import cats.effect.unsafe.IORuntime

object IOErrorHandling {

  // IO: pure, delay, defer
  // create failed effects
  val aFailedCompute: IO[Int] = IO.delay(throw new RuntimeException("Failed computation"))

  val aFailure = IO.raiseError(new RuntimeException("Failed computation"))

  // handle exceptions
  val dealWithIt = aFailure.handleErrorWith {
    case _: RuntimeException => IO.delay(println("Recovered!"))
    // add more cases
  }

  // turn into an Either
  val affectAsEither: IO[Either[Throwable, Int]] = aFailure.attempt

  // redeem: transform the failure and the success in one go
  val resultAString: IO[String] = aFailure.redeem(ex => s"Failed with $ex", res => s"Succeeded with $res")

  // redeemWith: transform the failure and the success in one go, with an IO
  val resultAsEffect: IO[Unit] = aFailure.redeemWith(ex => IO(println(s"Failed with $ex")), res => IO(println(s"Succeeded with $res")))

  def runExamples(using IORuntime): Unit = {
    // The 'END of the World!"

    //aFailure.unsafeRunSync()
    dealWithIt.unsafeRunSync()
    println(affectAsEither.unsafeRunSync())
    println(resultAString.unsafeRunSync())
    resultAsEffect.unsafeRunSync()
  }

  /**
   * Exercises
   */

  // 1 - construct potentially failed IOs from standard data types (Option, Try, Either)
  def option2IO[A](option: Option[A])(ifEmpty: Throwable): IO[A] = option.fold(IO.raiseError(ifEmpty))(IO.pure)

  def option2IO_v2[A](option: Option[A])(ifEmpty: Throwable): IO[A] = IO.fromOption(option)(ifEmpty)

  def try2IO[A](aTry: Try[A]): IO[A] = aTry.fold(IO.raiseError, IO.pure)

  def try2IO_v2[A](aTry: Try[A]): IO[A] = IO.fromTry(aTry)

  def either2IO[A](either: Either[Throwable, A]): IO[A] = either.fold(IO.raiseError, IO.pure)

  def either2IO_v2[A](either: Either[Throwable, A]): IO[A] = IO.fromEither(either)

  // 2 - handledError, handleErrorWith
  def handleIOError[A](io: IO[A])(handler: Throwable => A): IO[A] = io.handleError(handler)

  def handleIOErrorWith[A](io: IO[A])(handler: Throwable => IO[A]): IO[A] = io.handleErrorWith(handler)

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global // platform
    // The 'END of the World!"

    //runExamples

    println(option2IO(Some(111))(new RuntimeException("No value")).unsafeRunSync())
    //println(option2IO(None)(new RuntimeException("No value")).unsafeRunSync())

    println(try2IO(Try(222)).unsafeRunSync())
    //println(try2IO(Try(throw new RuntimeException("No value"))).unsafeRunSync())

    println(either2IO(Right(333)).unsafeRunSync())
    val anEitherFailureIO = either2IO(Left(new RuntimeException("No value")))
    //println(anEitherFailureIO.unsafeRunSync())

    handleIOError(anEitherFailureIO)(ex => println(s"Handled error: $ex")).unsafeRunSync()
    handleIOErrorWith(anEitherFailureIO)(ex => IO(println(s"Handled error again: $ex"))).unsafeRunSync()
  }
}
