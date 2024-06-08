package rtj.effectsio

import scala.concurrent.Future
import scala.util.Random

import cats.{Parallel, Traverse}
import cats.effect.{IO, IOApp}
import rtj.ec.syntax.*


object IOTraversal extends IOApp.Simple {

  given EC = EC()

  def heavyComputation(string: String): Int = {
    sleep(Random.nextInt(1000))
    string.split(" ").length
  }

  val workload: List[String] = List("I quite like CE", "Scala is great", "looking forward to some awesome stuff")

  def computeAsFuture(string: String): Future[Int] = Future(heavyComputation(string))

  def clunkyFutures(): Unit = {
    val futures: List[Future[Int]] = workload.map(computeAsFuture)
    // Future[List[Int]] would be hard to obtain
    futures.foreach(_.foreach(println))
  }

  // traverse
  //import cats.Traverse
  //import cats.instances.list.*
  //val listTraverse = Traverse[List]
  import cats.syntax.traverse.* // traverse

  def traverseFutures(): Unit = {
    val singleFuture: Future[List[Int]] = workload.traverse(computeAsFuture)
    // ^^^ this stores all the result
    singleFuture.foreach(println)
  }

  def computeAsIO(string: String): IO[Int] = IO(heavyComputation(string)).dbg
  val ios: List[IO[Int]] = workload.map(computeAsIO)
  val singleIO: IO[List[Int]] = workload.traverse(computeAsIO)

  // parallel traverse
  import cats.syntax.parallel.* // parTraverse
  val parallelSingleIO = workload.parTraverse(computeAsIO)

  /**
   * Exercises
   */
  // hint: use Traverse API
  def sequence[A](listOfIOs: List[IO[A]]): IO[List[A]] =
    Traverse[List].traverse(listOfIOs)(identity)

  // hard version
  def sequence_v2[F[_] : Traverse, A](listOfIOs: F[IO[A]]): IO[F[A]] =
    Traverse[F].traverse(listOfIOs)(identity)

  // parallel version
  def parSequence[A](wrapperOfIOs: List[IO[A]]): IO[List[A]] =
    //Parallel[IO].sequential(Traverse[List].traverse(listOfIOs)(Parallel[IO].parallel(_)))
    wrapperOfIOs.parTraverse(identity)

  // hard version
  def parSequence_v2[F[_] : Traverse, A](wrapperOfIOs: F[IO[A]]): IO[F[A]] =
    Parallel[IO].sequential(Traverse[F].traverse(wrapperOfIOs)(Parallel[IO].parallel(_)))

  // existing sequence API
  val singleIO_v2: IO[List[Int]] = ios.sequence

  // parallelisation
  val parallelSingleIO_v2: IO[List[Int]] = parSequence(ios)
  val parallelSingleIO_v3: IO[List[Int]] = ios.parSequence

  override def run: IO[Unit] = parSequence(ios).dbg.sum.dbg.void
}
