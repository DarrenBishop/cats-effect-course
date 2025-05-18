package rtj.coordination

import cats.effect.std.CountDownLatch
import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all.*
import rtj.all.*

import java.io.{File, FileWriter}
import scala.io.Source

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

  /**
   * Exercise: simulate a file downloader on multiple threads...
   */
  object FileServer {
    val fileChunksList = List(
      "I love Scala",
      "Cats Effect seems quite fun",
      "Never would I have thought I would do low-level concurrency WITH pure FP"
    )

    def getNumChunks: IO[Int] = IO(fileChunksList.length)
    def getFileChunk(i: Int): IO[String] = IO(fileChunksList(i))
  }

  def writeToFile(path: String, contents: String): IO[Unit] = Resource
    .make(IO(new FileWriter(new File(path))))(writer => IO(writer.close()))
    .use { writer => IO(writer.write(contents)) }

  def appendFileContents(fromPath: String, toPath: String): IO[Unit] = {
    val compositeResource = for {
      reader <- Resource.make(IO(Source.fromFile(fromPath)))(source => IO(source.close()))
      writer <- Resource.make(IO(new FileWriter(new File(toPath), true)))(writer => IO(writer.close()))
    } yield (reader, writer)

    compositeResource.use { (reader, writer) =>
      IO(reader.getLines().foreach(writer.write))
    }
  }

  /**
   *  - call file server API and get the number of chunks (n)
   *  - start a CDLatch
   *  - start n fibers which download a chunk of the file (use the file server's download chunk API)
   *  - block on the latch until each task has finished
   *  - after all chunks are done, stitch the files together under the same file on disk
   */
  def downloadFile(filename: String, destFolder: String): IO[Unit] = ???

  def run: IO[Unit] = sprint()
}
