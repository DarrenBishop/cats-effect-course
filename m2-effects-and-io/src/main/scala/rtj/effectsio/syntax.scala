package rtj.effectsio

import cats.effect.IO

trait PkgSyntax {

  def threadName: String = Thread.currentThread().getName

  def sleep(millis: Long): Unit = Thread.sleep(millis)

  extension [A](io: IO[A])
    def dbg: IO[A] = for {
      a <- io
      _ = System.err.println(s"[$threadName] $a")
    } yield a
    def delay(millis: Long): IO[A] = for {
      _ <- IO(sleep(millis))
      a <- io
    } yield a
    def wait(millis: Long): IO[A] = for {
      a <- io
      _ <- IO(sleep(millis))
    } yield a
}

object syntax extends PkgSyntax

export syntax.*
