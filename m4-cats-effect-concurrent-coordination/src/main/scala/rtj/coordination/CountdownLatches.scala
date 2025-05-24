package rtj.coordination

import cats.effect.kernel.Concurrent
import cats.effect.{Deferred, IO, IOApp, Ref, Resource}
import cats.syntax.all.*
import rtj.all.*

import java.io.{File, FileWriter}
import scala.io.Source
import scala.util.Random

/**
 * A CountDownLatch (CDL) is a coordination primitive initialized with a count.
 *
 * All fibers calling await() on the CDL are (semantically) blocked.
 * When the internal count of the latch reaches 0 (via release() calls from other fibers), all waiting fibers are unblocked.
 */
object CountdownLatches extends IOApp.Simple {

  //type CountDownLatch[F[_]] = cats.effect.std.CountDownLatch[F]
  //val CountDownLatch = cats.effect.std.CountDownLatch
  type CountDownLatch[F[_]] = my.CountDownLatch[F]
  val CountDownLatch: my.CountDownLatch.type = my.CountDownLatch

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
    //_ <- latch.release(IO.void("GO GO GO!").delay(200.millis)) // gun firing
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

  def readerIO(fromPath: String) = Resource.make(IO(Source.fromFile(fromPath)))(source => IO(source.close()))

  def writeToFile(path: String, contents: String): IO[Unit] = Resource
    .make(IO(new FileWriter(new File(path))))(writer => IO(writer.close()))
    .use { writer => IO(writer.write(contents)) }

  def appendFileContents(fromPath: String, toPath: String): IO[Unit] = {
    val compositeResource = for {
      reader <- readerIO(fromPath)
      writer <- Resource.make(IO(new FileWriter(new File(toPath), true)))(writer => IO(writer.close()))
    } yield (reader, writer)

    compositeResource.use { (reader, writer) =>
      IO(reader.getLines().map(_ + "\n").foreach(writer.write))
    }
  }

  def downloadFilePart(part: String, partNum: Int, latch: CountDownLatch[IO]): IO[Unit] = for {
    chunk <- FileServer.getFileChunk(partNum)
    duration = Random.nextInt(1000).millis
    _ <- IO.dbg(s"Download of part $part took $duration").delay(duration)
    _ <- writeToFile(part, chunk)
    _ <- latch.release
  } yield ()

  /**
   *  - call file server API and get the number of chunks (n)
   *  - start a CDLatch
   *  - start n fibers which download a chunk of the file (use the file server's download chunk API)
   *  - block on the latch until each task has finished
   *  - after all chunks are done, stitch the files together under the same file on disk
   */
  def downloadFile(filename: String, destFolder: String): IO[Unit] = for {
    numOfChunks <- FileServer.getNumChunks
    path = s"$destFolder/$filename"
    parts = (0 untilL numOfChunks).map { i => s"$path.part$i" -> i }
    latch <- CountDownLatch[IO](numOfChunks)
    _ <- parts.parTraverse { downloadFilePart(_, _, latch) }
    _ <- latch.await
    _ <- IO.dbg(s"All chunks downloaded; stitching to $filename...")
    _ <- parts._1F.traverse { part =>
      IO.dbg(s"...stitching $part") >> appendFileContents(part, path)
    }
  } yield ()

  def demoDownloadFile: IO[Unit] = for {
    tmp <- IO(getClass.getClassLoader.getResource(".").getFile).dbg
    //tmp <- IO(Files.createTempDirectory("rock-the-jvm").toString).dbg
    filename = "CountDownLatch"
    filepath = s"$tmp/$filename"
    _ <- IO(new File(filepath).delete)
    _ <- downloadFile(filename, tmp)
    _ <- IO.dbg(s"All chunks stitched; checking $filename...")
    _ <- readerIO(s"$tmp/$filename")
      .use { reader => IO(reader.getLines().foreach(println)) }
  } yield ()

  def run: IO[Unit] = sprint()
  //def run: IO[Unit] = demoDownloadFile
}

/**
  * Exercise II: implement your own Count Down Latch with Ref and Deferred
  */
object my {
  trait CountDownLatch[F[_]] {
    def release: F[Unit]
    def release(effect: F[Unit]): F[Unit]
    def await: F[Unit]
  }

  object CountDownLatch {
    def apply[F[_]: {Concurrent, Uncancelable}](n: Int): F[CountDownLatch[F]] = for {
      count <- Ref[F].of(n)
      signal <- Deferred[F, Unit]
    } yield new CountDownLatch {
      def release(onReleased: F[Unit]): F[Unit] = count.flatModify {
        //case 1 => (0, onReleased *> signal.complete(()).void)
        case 1 => (0, signal.complete(()).void <* onReleased) // want an atomic-semantics on the modify-effects
        case n => (n - 1, Concurrent[F].unit)
      }.uncancelable
      def release: F[Unit] = release(Concurrent[F].unit)
      def await: F[Unit] = signal.get
    }
  }
}
