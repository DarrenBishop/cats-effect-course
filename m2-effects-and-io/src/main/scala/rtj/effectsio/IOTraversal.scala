package rtj.effectsio

import scala.concurrent.Future
import scala.util.Random

import cats.Traverse
import cats.effect.{IO, IOApp}
import rtj.ec.syntax.*


object IOTraversal extends IOApp.Simple {

  given EC = EC()

  def heavyComputation(string: String): Int = {
    sleep(Random.nextInt(1000))
    string.split(" ").length
  }

  val workload: List[String] = List("I quite like CE", "Scala is great", "looking forward to some awesome stuff")

  def computedAsFuture(string: String): Future[Int] = Future(heavyComputation(string))

  def clunkyFutures(): Unit = {
    val futures: List[Future[Int]] = workload.map(computedAsFuture)
    // Future[List[Int]] would be hard to obtain
    futures.foreach(_.foreach(println))
  }

  // traverse
  //import cats.Traverse
  //import cats.instances.list.*
  //val listTraverse = Traverse[List]
  import cats.syntax.traverse.* // traverse

  def traverseFutures(): Unit = {
    val singleFuture: Future[List[Int]] = workload.traverse(computedAsFuture)
    // ^^^ this stores all the result
    singleFuture.foreach(println)
  }

  def computeAsIO(string: String): IO[Int] = IO(heavyComputation(string)).dbg
  val singleIO: IO[List[Int]] = workload.traverse(computeAsIO)

  // parallel traverse
  import cats.syntax.parallel.* // parTraverse
  val parallelSingleIO = workload.parTraverse(computeAsIO)

  /**
   * Exercises
   */
  // hint: use Traverse API
  def sequence[A](listOfIOs: List[IO[A]]): IO[List[A]] = ???

  // hard version
  def sequence_v2[F[_] : Traverse, A](listOfIOs: F[IO[A]]): IO[F[A]] = ???

  // parallel version
  def parSequence[A](listOfIOs: List[IO[A]]): IO[List[A]] = ???

  // hard version
  def parSequence_v2[F[_] : Traverse, A](listOfIOs: F[IO[A]]): IO[F[A]] = ???

  override def run: IO[Unit] = parallelSingleIO.dbg.sum.dbg.void
}
