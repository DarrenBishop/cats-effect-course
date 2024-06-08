package rtj.effectsio

import scala.io.StdIn

import cats.effect.{ExitCode, IO, IOApp}


object IOApps {

  val program = for {
    line <- IO(StdIn.readLine)
    _ <- IO(println(s"You've just written: $line"))
  } yield ()
}

import rtj.effectsio.IOApps.*

object TestApp {

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.given

    program.unsafeRunSync()
  }
}

object FirstCEApp extends IOApp {
  override def run(args: List[String]) = program.as(ExitCode.Success)
}

object MyFirstSimpleCEApp extends IOApp.Simple {
  override def run: IO[Unit] = program
}
