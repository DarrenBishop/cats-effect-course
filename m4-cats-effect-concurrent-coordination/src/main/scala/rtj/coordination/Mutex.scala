package rtj.coordination

import cats.effect.{Deferred, IO, IOApp, Ref}
import cats.syntax.all.*
import rtj.all.*

import scala.util.Random

abstract class Mutex {
  def acquire: IO[Unit]
  def release: IO[Unit]
}

object Mutex extends IOApp.Simple {
  def create: IO[Mutex] = ??? // TODO
  def mutex[A](f: Mutex => IO[A]): IO[A] = create.flatMap(f)

  // App

  def criticalTask(): IO[Int] = IO.sleep(1.second) >> IO(Random.nextInt(100))

  val ids: List[Int] = (1 to 10).toList

  def createNonLockingTask(id: Int): IO[Int] = for {
    _ <- IO.void(s"[task $id] working...")
    res <- criticalTask()
    _ <- IO.void(s"[task $id] got result: $res")
  } yield res

  def demoNonLockingTask(): IO[List[Int]] = ids.parTraverse(createNonLockingTask)

  def createLockingTask(id: Int, mutex: Mutex): IO[Int] = for {
    _ <- IO.void(s"[task $id] waiting for permission...")
    _ <- mutex.acquire // blocks if the mutex has been acquired by some other fibre
    // critical section start
    _ <- IO.void(s"[task $id] working...")
    res <- criticalTask()
    _ <- IO.void(s"[task $id] got result: $res")
    // critical section end
    _ <- IO.void(s"[task $id] permissions relinquished")
    _ <- mutex.release
  } yield res

  def demoLockingTask(): IO[List[Int]] = mutex { mtx => ids.parTraverse(createLockingTask(_, mtx)) }

  //def run: IO[Unit] = demoNonLockingTask().dvoid
  def run: IO[Unit] = demoLockingTask().dvoid
}