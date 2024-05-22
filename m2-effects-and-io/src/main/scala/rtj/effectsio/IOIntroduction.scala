package rtj.effectsio

import scala.io.StdIn

import cats.effect.IO


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
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _ <- IO(println(s"$line1\n$line2"))
  yield ()

  // mapN - combine IO effects as tuples (from Apply)
  import cats.syntax.apply.*
  val combinedMeaningOfLife: IO[Int] = (aPureIO, improvedMeaningOfLife).mapN(_ + _)

  def smallProgram_v2(): IO[Unit] = (IO(StdIn.readLine()), IO(StdIn.readLine())).mapN(_ + _).map(println)

  def main(args: Array[String]): Unit = {

    import cats.effect.unsafe.implicits.global // platform
    // The 'END of the World!"
    println(aDelayedIO.unsafeRunSync())
    smallProgram().unsafeRunSync()
    smallProgram_v2().unsafeRunSync()
  }
}
