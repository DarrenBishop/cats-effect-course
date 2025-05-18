package rtj.coordination

import cats.effect.std.CountDownLatch
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import rtj.all.*

/**
 * A CountDownLatch (CDL) is a coordination primitive initialized with a count.
 *
 * All fibers calling await() on the CDL are (semantically) blocked.
 * When the internal count of the latch reaches 0 (via release() calls from other fibers), all waiting fibers are unblocked.
 */
object CountdownLatches extends IOApp.Simple {

  def announcer(latch: CountDownLatch[IO]): IO[Unit] = for {
    _ <- IO.pause("Starting race shortly...", 2.seconds)
    _ <- IO.pause("5...")
    _ <- latch.release
    _ <- IO.pause("4...")
    _ <- latch.release
    _ <- IO.pause("3...")
    _ <- latch.release
    _ <- IO.pause("2...")
    _ <- latch.release
    _ <- IO.pause("1...")
    _ <- latch.release // gun firing
    _ <- IO.dbg("GO GO GO!")
  } yield ()

  def craeteRunner(id: Int, latch: CountDownLatch[IO]): IO[Unit] = for {
    _ <- IO.dbg(s"[runner $id] waiting for signal...")
    _ <- latch.await // block the fiber until the count reaches 0
    _ <- IO.dbg(s"[runner $id] RUNNING!")
  } yield ()

  def sprint(): IO[Unit] = for {
    latch <- CountDownLatch[IO](5)
    announcerFib <- announcer(latch).start
    _ <- (1 toL 10).parTraverse(craeteRunner(_, latch))
    _ <- announcerFib.join
  } yield ()

  def run: IO[Unit] = sprint()
}
