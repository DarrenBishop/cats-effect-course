package rtj.ec

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

import cats.Foldable
import cats.effect.IO
import cats.syntax.all.*


trait PkgSyntax {
  import rtj.syntax.*
  
  def shutdownAll(): Unit = Context.shutdownAll()

  def ready[R](fR: Future[R])(implicit duration: FiniteDuration): Future[R] = Await.ready(fR, duration)

  def result[R](fR: Future[R])(implicit duration: FiniteDuration): R = Await.result(fR, duration)

  def runAsyncF[R](af: EC => Future[R])(implicit duration: FiniteDuration): Future[R] = {
    val ec = EC()
    val fR = Await.ready(af(ec), duration)
    ec.shutdown()
    fR
  }
  
  final def runAsync[R](af: EC => Future[R])(implicit duration: FiniteDuration): R = runAsyncF[R](af)(duration).value match {
    case Some(Success(result)) => result
    case Some(Failure(ex)) => throw ex
    case None => throw new IllegalStateException("This should never happen!")
  }

  final def printlnAsync(af: EC => Future[Any])(implicit duration: FiniteDuration): Unit = runAsyncF(af)(duration).value match {
    case Some(Success(result)) => println(result)
    case Some(Failure(ex)) => println(s"Throw: $ex")
    case None => throw new IllegalStateException("This should never happen!")
  }

  def threadName: String = Thread.currentThread().getName

  def sleep(millis: Long): Unit = Thread.sleep(millis)

  extension (ioo: IO.type)
    def dbg(message: => String): IO[String] = IO(message).dbg

  extension [A](io: IO[A])
    def dbg: IO[A] = io
      .flatTap(a => IO.println(s"[$threadName] $a"))
      .handleErrorWith(ex => IO.println(s"[$threadName] ${ex.getMessage}") >> IO.raiseError(ex))
    def debug: IO[A] = dbg
    def delay(millis: Long): IO[A] = IO(sleep(millis)) >> io
    def wait(millis: Long): IO[A] = io <* IO(sleep(millis))
    def silence: IO[Unit] = io.attempt.void

  extension [C[_]: Foldable, A](io: IO[C[A]])
    def sum(using Numeric[A]): IO[A] = io.map(Foldable[C].foldLeft(_, Numeric[A].zero)(Numeric[A].plus))


  def !? [A](msg: String): IO[A] = IO.raiseError[A](!??(msg))
  def canceled = IO("Fiber canceled")
  def !![A] = canceled.dbg >>= !?
}

object syntax extends PkgSyntax

export syntax.*
