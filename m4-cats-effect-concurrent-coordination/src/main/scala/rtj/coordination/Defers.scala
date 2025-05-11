package rtj.coordination

import cats.effect.{Deferred, IO, IOApp, Ref}
import cats.syntax.all.*
import rtj.all.*

object Defers extends IOApp.Simple {

  /**
    * deferred is a primitive for waiting for an effect, while some other
    * effect completes it with a value
    */
  val aDeferred: IO[Deferred[IO, Int]] = Deferred[IO, Int]
  val aDeferred_v2: IO[Deferred[IO, Int]] = IO.deferred[Int]

  /**
    * get blocks the calling fiber (semantically) until some other fiber
    * completes the Deferred with a value
    */
  val reader: IO[Int] = aDeferred.flatMap { signal =>
    signal.get // blocks the fiber
  }

  val writer: IO[Boolean] = aDeferred.flatMap { signal =>
    signal.complete(42)
  }

  def demoDeferred(): IO[Unit] = {
    def consumer(signal: Deferred[IO, Int]): IO[Unit] = for {
      _ <- IO.dbg("consuming...")
      value <- signal.get
      _ <- IO.dbg(s"consumed value $value")
    } yield ()

    def producer(signal: Deferred[IO, Int]): IO[Unit] = for {
      _ <- IO.dbg("producing...")  <* IO.sleep(1.second)
      value = 42
      _ <- signal.complete(value) <* IO.dbg(s"produced $value")
    } yield ()

    for {
      signal <- IO.deferred[Int]
      _ <- (consumer(signal), producer(signal)).parTupled
    } yield ()
  }

  /**
    * simulate downloading some content
    */
  val marker = "<EOF>"
  val fileParts = List("I ", "love S", "cala", " with Cat", s"s Effect!" + marker)

  def fileNotifierWithRef(): IO[Unit] = {
    def downloadFile(contentRef: Ref[IO, String]): IO[Unit] =
      fileParts.traverse_(part => IO.dbg(s"[downloader] got '$part'") >> IO.sleep(500.millis) >> contentRef.update(_ + part))

    def notifyFileComplete(contentRef: Ref[IO, String]): IO[Unit] = for {
      file <- contentRef.get
      _ <- if (file.endsWith(marker)) IO.dbg("[notifier] File download complete") >> IO.dbg(file.stripSuffix(marker))
      else IO.dbg("[notifier] downloading...") >> IO.sleep(100.millis) >> notifyFileComplete(contentRef) // busy wait
    } yield ()

    for {
      contentRef <- IO.ref("")
      _ <- (downloadFile(contentRef), notifyFileComplete(contentRef)).parTupled
    } yield ()
  }

  /**
    * deferred works miracles for waiting
    */
  def fileNotifierWithDeferred(): IO[Unit] = {
    def downloadPart(part: String, contentRef: Ref[IO, String], signal: Deferred[IO, String]): IO[Unit] = for {
      _ <- IO.sleep(100.millis)
      _ <- IO.dbg(s"[downloader] got '$part'")
      _ <- IO.sleep(400.millis)
      _ <- contentRef.update(_ + part)
      _ <- IO(part.endsWith(marker)).ifM(
        contentRef.updateAndGet(_.stripSuffix(marker)).flatMap(signal.complete),
        IO.unit
      )
    } yield ()

    def downloadFile(contentRef: Ref[IO, String], signal: Deferred[IO, String]): IO[Unit] =
      fileParts.traverse(downloadPart(_, contentRef, signal)).void

    def notifyFileComplete(signal: Deferred[IO, String]): IO[Unit] = for {
      _ <- IO.void("[notifier] File downloading...")
      content <- signal.get
      _ <- IO.void("[notifier] File download complete")
      _ <- IO.void(content)
    } yield ()

    for {
      contentRef <- IO.ref("")
      signal <- IO.deferred[String]
      _ <- (notifyFileComplete(signal), downloadFile(contentRef, signal)).parTupled
    } yield ()
  }

  //def run: IO[Unit] = demoDeferred()
  //def run: IO[Unit] = fileNotifierWithRef()
  def run: IO[Unit] = fileNotifierWithDeferred()
}
