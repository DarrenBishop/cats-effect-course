package rtj.ec

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

trait PkgSyntax {
  import types.*
  
  def shutdownAll(): Unit = Context.shutdownAll()

  def ready[R](fR: Future[R])(using duration: FiniteDuration): Future[R] = Await.ready(fR, duration)

  def result[R](fR: Future[R])(using duration: FiniteDuration): R = Await.result(fR, duration)

  def runAsyncF[R](af: EC => Future[R])(using duration: FiniteDuration): Future[R] = {
    val ec = EC()
    val fR = Await.ready(af(ec), duration)
    ec.shutdown()
    fR
  }
  
  final def runAsync[R](af: EC => Future[R])(using duration: FiniteDuration): R = runAsyncF[R](af).value match {
    case Some(Success(result)) => result
    case Some(Failure(ex)) => throw ex
    case None => throw new IllegalStateException("This should never happen!")
  }

  final def printlnAsync(af: EC => Future[Any])(using duration: FiniteDuration): Unit = runAsyncF(af).value match {
    case Some(Success(result)) => println(result)
    case Some(Failure(ex)) => println(s"Throw: $ex")
    case None => throw new IllegalStateException("This should never happen!")
  }

  def sleep(millis: Long): Unit = Thread.sleep(millis)
}

object syntax extends PkgSyntax

export syntax.*
