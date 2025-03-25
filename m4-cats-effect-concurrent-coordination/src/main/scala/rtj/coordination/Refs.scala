package rtj.coordination

import cats.effect.{IO, IOApp, Ref}
import cats.syntax.parallel.*
import rtj.all.*

object Refs extends IOApp.Simple {

  /**
    * ref = purely functional atomic reference
    */
  val atomicMol: IO[Ref[IO, Int]] = Ref[IO].of(42)
  val atomicMol_v2: IO[Ref[IO, Int]] = Ref.of(42)
  val atomicMol_v3: IO[Ref[IO, Int]] = IO.ref(42)

  /**
    * modifying is an effect
    */
  val increaseMol: IO[Unit] = atomicMol.flatMap { ref =>
    ref.set(43) // thread-safe
  }

  /**
    * obtain a value
    */
  val mol: IO[Int] = atomicMol.flatMap { ref =>
    ref.get // thread-safe
  }

  /**
    * get the old value, set a new value
    */
  val getAndSetMol: IO[Int] = atomicMol.flatMap { ref =>
    ref.getAndSet(43)
  }

  /**
    * updating with a function
    */
  val fMol: IO[Unit] = atomicMol.flatMap { ref =>
    ref.update(value => value * 10)
  }

  /**
    * update and get the NEW value
    * can also use getAndUpdate to get the OLD value
    */
  val updateMol: IO[Int] = atomicMol.flatMap { ref =>
    ref.updateAndGet(value => value * 10) // get the new value
  }

  /**
    * modifying with a function returning a different type
    */
  val modifiedMol: IO[String] = atomicMol.flatMap { ref =>
    ref.modify(value => (value * 10, s"my current value is $value"))
  }

  /**
    * why: concurrent & thread-safe reads/write over shared values, in a purely functional way
    */
  def demoConcurrentWorkImpure(): IO[Unit] = {
    var count = 0

    def task(workload: String): IO[Unit] = {
      val wordCount = workload.split(" ").length

      for {
        _ <- IO.dbg(s"Counting words for '$workload': $wordCount")
        newCount <- IO(count + wordCount)
        _ <- IO.dbg(s"New total: $newCount")
        _ <- IO(count += newCount)
      } yield ()
    }

    List("I love Cats Effect", "This ref thing is useless", "Darren writes a loft of code")
      .map(task)
      .parSequence
      .void
  }

  /**
    * Drawbacks:
    * - hard to read/write
    * - mix pure/impure code
    * - NOT THREAD SAFE
    */
  def demoConcurrentWorkPure(): IO[Unit] = {
    def task(total: Ref[IO, Int])(workload: String): IO[Unit] = {
      val wordCount = workload.split(" ").length

      for {
        _ <- IO.dbg(s"Counting words for '$workload': $wordCount")
        newCount <- total.updateAndGet(current => current + wordCount)
        _ <- IO.dbg(s"New total: $newCount")
      } yield ()
    }

    for {
      initialCount <- IO.ref(0)
      _ <- List("I love Cats Efect", "This ref thing is useless", "Darren writes a loft of code")
        .map(task(initialCount))
      .parSequence
      total <- initialCount.get
        _ <- IO.dbg(s"Final total $total")
    } yield ()
  }

  def run: IO[Unit] = demoConcurrentWorkPure()
}
