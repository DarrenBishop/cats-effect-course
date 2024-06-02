package rtj.effectsio

import scala.util.Try
import cats.effect.IO

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

  def runExamples: Unit = {
    import cats.effect.unsafe.implicits.global // platform
    // The 'END of the World!"

    //aFailure.unsafeRunSync()
    dealWithIt.unsafeRunSync()
    println(affectAsEither.unsafeRunSync())
    println(resultAString.unsafeRunSync())
    resultAsEffect.unsafeRunSync()
  }

  def main(args: Array[String]): Unit = {
    runExamples
  }
}
