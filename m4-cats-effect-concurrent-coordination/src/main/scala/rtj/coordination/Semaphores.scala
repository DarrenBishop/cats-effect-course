package rtj.coordination

import cats.effect.std.Semaphore
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import rtj.all.*

object Semaphores extends IOApp.Simple {

  given Seed(123)

  val semaphore: IO[Semaphore[IO]] = Semaphore(2) // 2 total permits

  // example: limiting the number of concurrent sessions on a server
  def doWorkWhileLoggedIn(using Rng[IO]): IO[Int] = rng.nextIntBounded(Int.MaxValue).delay(1.second)

  def login(id: Int, sem: Semaphore[IO])(using Rng[IO]): IO[Int] = for {
    _ <- IO.dbg(s"[session $id] waiting to log-in...")
    _ <- sem.acquire
    // critical section start
    _ <- IO.dbg(s"[session $id] logged in, working...")
    result <- doWorkWhileLoggedIn
    _ <- IO.dbg(s"[session $id] done: $result, logging out...")
    // critical section end
    _ <- sem.release
  } yield result

  def demoSemaphore(): IO[Unit] = for {
    given Rng[IO] <- random[IO]
    sem <- Semaphore[IO](2)
    userFib1 <- login(1, sem).start
    userFib2 <- login(2, sem).start
    userFib3 <- login(3, sem).start
    _ <- userFib1.join
    _ <- userFib2.join
    _ <- userFib3.join
  } yield ()

  def weightedLogin(id: Int, requiredPermits: Int, sem: Semaphore[IO])(using Rng[IO]): IO[Int] = for {
    _ <- IO.dbg(s"[session $id] waiting to log-in...")
    _ <- sem.acquireN(requiredPermits)
    // critical section start
    _ <- IO.dbg(s"[session $id] logged in, working...")
    result <- doWorkWhileLoggedIn
    _ <- IO.dbg(s"[session $id] done: $result, logging out...")
    // critical section end
    _ <- sem.releaseN(requiredPermits)
  } yield result

  def demoWeightedSemaphore(): IO[Unit] = for {
    given Rng[IO] <- random[IO]
    sem <- Semaphore[IO](3)
    userFib1 <- weightedLogin(1, 1, sem).start
    userFib2 <- weightedLogin(2, 2, sem).start
    userFib3 <- weightedLogin(3, 3, sem).start
    _ <- userFib1.join
    _ <- userFib2.join
    _ <- userFib3.join
  } yield ()

  /**
   * Exercise
   *   1. find out if there's something wrong with this code
   *   2. why?
   *   3. fix it
   */
  
  // Semaphore with 1 permit == mutex
  val mutex = Semaphore[IO](1)
  
  val users: IO[List[Int]] = random[IO].flatMapU {
    (1 to 10).toList.parTraverse { id =>
      for {
        sem <- mutex
        _ <- IO.dbg(s"[session $id] waiting to log-in...")
        _ <- sem.acquire
        // critical section start
        _ <- IO.dbg(s"[session $id] logged in, working...")
        result <- doWorkWhileLoggedIn
        _ <- IO.dbg(s"[session $id] done: $result, logging out...")
        // critical section end
        _ <- sem.release
      } yield result
    }
  }

  //def run: IO[Unit] = demoSemaphore()
  //def run: IO[Unit] = demoWeightedSemaphore()
  def run: IO[Unit] = users.dvoid
}
