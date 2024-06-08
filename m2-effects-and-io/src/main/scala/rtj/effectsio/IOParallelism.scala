package rtj.effectsio

import cats.Parallel
import cats.syntax.all.*
import cats.effect.{IO, IOApp}


object IOParallelism extends IOApp.Simple {

  // IOs are usually sequential
  val anisIO = IO(s"[$threadName] Ani")
  val kamransIO = IO(s"[$threadName] Kamran")

  val composedIO = for {
    ani <- anisIO
    kamran <- kamransIO
  } yield s"$ani and $kamran love Rock the JVM"

  //debugX method is exported to the package's top-level scope, so is automatically available
  //import cats.syntax.apply.*
  val meaningOfLife: IO[Int] = IO.delay(42)
  val favLang: IO[String] = IO.delay("Scala")
  val goalInLife = (meaningOfLife, favLang).mapN((num, string) => s"My goal in life is $num and $string")

  // parallelism in IOs
  // convert a sequential IO to parallel IO
  val meaningOfLifePar: IO.Par[Int] = Parallel[IO].parallel(meaningOfLife.dbg)
  val favLangPar: IO.Par[String] = Parallel[IO].parallel(favLang.dbg)
  val goalInLifePar: IO.Par[String] = (meaningOfLifePar, favLangPar).mapN((num, string) => s"My goal in life is $num and $string")
  // turn back to sequential
  val goalInLife_v2 = Parallel[IO].sequential(goalInLifePar)

  // shorthand
  //import cats.syntax.parallel.*
  val goalInLife_v3 = (meaningOfLife.dbg, favLang.dbg).parMapN((num, string) => s"My goal in life is $num and $string")

  // regarding failure
  val aFailure: IO[String] = IO.raiseError(new RuntimeException("I can't do this!"))
  // compose success + failure
  val parallelFailure = (meaningOfLife.dbg, aFailure.dbg).parMapN(_ + _)
  //compose a failure + failure
  val anotherFailure = IO.raiseError(new RuntimeException("Second failure"))
  val twoFailures = (aFailure.dbg, anotherFailure.dbg.delay(1000)).parMapN(_ + _)

  override def run: IO[Unit] = twoFailures.dbg.void
}
