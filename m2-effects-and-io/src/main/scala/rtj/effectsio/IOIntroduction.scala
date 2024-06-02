package rtj.effectsio

import scala.io.StdIn
import cats.effect.IO
import cats.effect.unsafe.IORuntime


object IOIntroduction {

  // IO
  val aPureIO: IO[Int] = IO.pure(42) // arg that should not have side-effects

  val aDelayedIO: IO[Int] = IO.delay {
    println("I'm producing an integer")
    54
  }

  val aDelayedIO_v2: IO[Int] = IO {
    println("I'm producing an integer")
    54
  }

  // map, flatMap
  val improvedMeaningOfLife: IO[Int] = aPureIO.map(_ * 2)
  val printedMeaniningOfLife: IO[Unit] = aPureIO.flatMap { mol => IO { println(mol) } }

  def smallProgram(): IO[Unit] = for
    _ <- IO(println("Please enter two lines:"))
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _ <- IO(println(s"$line1\n$line2"))
  yield ()

  // mapN - combine IO effects as tuples (from Apply)
  import cats.syntax.apply.*
  val combinedMeaningOfLife: IO[Int] = (aPureIO, improvedMeaningOfLife).mapN(_ + _)

  def smallProgram_v2(): IO[Unit] =
    IO(println("Please enter two lines:")) >> (IO(StdIn.readLine()), IO(StdIn.readLine())).mapN(_ + _).map(println)

  def runExamples(using IORuntime): Unit = {
    println(aDelayedIO.unsafeRunSync())
    smallProgram().unsafeRunSync()
    smallProgram_v2().unsafeRunSync()
  }

  /**
   * Exercises
   */

  // 1 - sequence two IOs and take the result of the last one
  // hint: use flatMap
  def sequenceTakeLast[A, B](ioa: IO[A], iob: IO[B]): IO[B] = ???

  // 2 - sequence two IOs and take the result of the first one
  // hint: use flatMap
  def sequenceTakeFirst[A, B](ioa: IO[A], iob: IO[B]): IO[A] = ???

  // 3 - repeat an IO effect forever
  // hint: use flatMap
  def forever[A](io: IO[A]): IO[A] = ???

  // 4 - convert an IO to a different type
  // hint: use map
  def convert[A, B](ioa: IO[A], value: B): IO[B] = ???

  // 5 - discard value inside an IO, just return unit
  def discard[A](ioa: IO[A]): IO[Unit] = ???

  // 6 - fix stack recursion
  def sum(n: Int): Int =
    if n <= 0 then 0
    else n + sum(n - 1)

  def sumIO(n: Int): IO[Int] = ???

  // 7 (hard) - write fibinacci IO that does NOT crash on recursion
  // hints: use recursion, ignore exponential complexity, use flatMap heavily
  def fibonacci(n: Int): IO[BigInt] = ???

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global // platform
    // The 'END of the World!"

    runExamples
  }
}
