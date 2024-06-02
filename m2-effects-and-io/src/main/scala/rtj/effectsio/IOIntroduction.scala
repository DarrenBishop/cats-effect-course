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
   * exercises
   */

  // 1 - sequence two IOs and take the result of the last one
  // hint: use flatMap
  def sequenceTakeLast[A, B](ioa: IO[A], iob: IO[B]): IO[B] =
    ioa.flatMap(_ => iob)

  def sequenceTakeLast_v2[A, B](ioa: IO[A], iob: IO[B]): IO[B] = for
    _ <- ioa
    b <- iob
  yield b

  def sequenceTakeLast_v3[A, B](ioa: IO[A], iob: IO[B]): IO[B] =
    ioa *> iob // "andThen"

  def sequenceTakeLast_v4[A, B](ioa: IO[A], iob: IO[B]): IO[B] =
    ioa >> iob // "andThen" with by-name iob i.e. lazy

  // 2 - sequence two IOs and take the result of the first one
  // hint: use flatMap
  def sequenceTakeFirst[A, B](ioa: IO[A], iob: IO[B]): IO[A] =
    ioa.flatMap(a => iob.map(_ => a))

  def sequenceTakeFirst_v2[A, B](ioa: IO[A], iob: IO[B]): IO[A] = for
    a <- ioa
    _ <- iob
  yield a

  def sequenceTakeFirst_v3[A, B](ioa: IO[A], iob: IO[B]): IO[A] =
    ioa <* iob

  // 3 - repeat an IO effect forever
  // hint: use flatMap
  def forever[A](io: IO[A]): IO[A] = io.flatMap(_ => forever(io))

  def forever_v2[A](io: IO[A]): IO[A] = io >> forever_v2(io) // same

  def forever_v3[A](io: IO[A]): IO[A] = io *> forever_v3(io) // same, but not stack-safe

  def forever_v4[A](io: IO[A]): IO[A] = io.foreverM

  // 4 - convert an IO to a different type
  // hint: use map
  def convert[A, B](ioa: IO[A], value: B): IO[B] = ioa.map(_ => value)

  def convert_v2[A, B](ioa: IO[A], value: B): IO[B] = ioa.as(value)

  // 5 - discard value inside an IO, just return unit
  def discard[A](ioa: IO[A]): IO[Unit] = convert(ioa, ())
  def discard_v1[A](ioa: IO[A]): IO[Unit] = ioa.as(())
  def discard_v2[A](ioa: IO[A]): IO[Unit] = ioa.void

  // 6 - fix stack recursion
  def sum(n: Int): Int =
    if n <= 0 then 0
    else n + sum(n - 1)

  def sumIO(n: Int): IO[Int] = IO(n).flatMap {
    case n if n <= 0 => IO(n)
    case n           => sumIO(n - 1).map(_ + n)
  }

  // 7 (hard) - write fibinacci IO that does NOT crash on recursion
  // hints: use recursion, ignore exponential complexity, use flatMap heavily
  def fibonacci(n: Int): IO[BigInt] = {
    def aux(n: Int): IO[(BigInt, BigInt)] = IO(n).flatMap {
      case 0 => IO((BigInt(0), BigInt(0)))
      case 1 => IO((BigInt(1), BigInt(0)))
      case _ => aux(n - 1).flatMap {
        case (l1, l2) => IO((l1 + l2, l1))
      }
    }

    aux(n).map(_._1)
  }

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global // platform
    // The 'END of the World!"

    //runExamples

    println(sequenceTakeLast(IO("first"), IO("last")).unsafeRunSync())
    println(sequenceTakeFirst(IO("first"), IO("last")).unsafeRunSync())
    //println(forever(IO(println("Hello!"))).unsafeRunSync())
    println(convert(IO("Hello!"), "Goodbye!".toList).unsafeRunSync())
    println(discard(IO("Hello!")).unsafeRunSync())

    val summed = 100000
    //println(s"sum($summed): ${sum(summed)}")
    println(s"sumIO($summed): ${sumIO(summed).unsafeRunSync()}")
    List(1, 10, 100, 1000, 10_000, 100_000).foreach { n =>
      println(s"IO Fibonacci $n: ${fibonacci(n).unsafeRunSync()}")
    }
  }
}
