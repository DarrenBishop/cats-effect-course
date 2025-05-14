package rtj.coordination

import cats.effect.Outcome.*
import cats.effect.{Deferred, Fiber, IO, IOApp, Outcome, OutcomeIO, Ref}
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

  /**
   * Exercise 1:
   * - (medium) write a small alarm notification with two simultaneous IOs
   *    - one that increments a counter every second (a clock)
   *    - one that waits for the counter to become 10, then prints a message "time's up!"
   */

  def alarm(): IO[Unit] = {
    def tick(time: Ref[IO, Int], signal: Deferred[IO, Int]): IO[Unit] = for {
      _ <- IO.sleep(1.second)
      nt <- time.updateAndGet(_ + 1)
      _ <- IO.void(s"the time is $nt")
      _ <- IO(nt == 10).ifM(signal.complete(nt), tick(time, signal))
    } yield ()

    def notification(signal: Deferred[IO, Int]): IO[Unit] =
    signal.get >> IO.void("time's up!")

    for {
      time <- IO.ref(0)
      signal <- IO.deferred[Int]
      _ <- (notification(signal), tick(time, signal)).parTupled
    } yield ()
  }

  /**
   * Exercise 2:
   * - (mega hard) implement racePair with Deferred.
   *    - use a Deferred which can hold an Either[outcome for the ioa, outcome for iob]
   *    - start two fibers, one for each IO
   *    - on completion (with any status), each IO needs to complete that Deferred
   *       (hint: use a finalizer from the Resources lesson)
   *       (hint 2: use a guarantee call to make sure the fibers complete the Deferred)
   *    - what do you do in case of cancellation (the hardest part)?
   */

  type RaceResult[F[_], A, B] = Either[
    (Outcome[F, Throwable, A], Fiber[F, Throwable, B]), // (winner outcome, loser fiber)
    (Fiber[F, Throwable, A], Outcome[F, Throwable, B]) // (loser fiber, winner outcome)
  ]

  type RaceResultIO[A, B] = RaceResult[IO, A, B]

  def ourRacePair[A, B](ioa: IO[A], iob: IO[B]): IO[RaceResultIO[A, B]] = for {
    race <- IO.deferred[RaceResultIO[A, B]]
    fiba <- ioa.onCancel(IO.void("IO-A canceled!")).start
    fibb <- iob.onCancel(IO.void("IO-B canceled!")).start
    fibra <- fiba.join.guaranteeCase {
      case Succeeded(iooa) => iooa >>= (oa => race.complete((oa, fibb).asLeft).void)
      case Errored(err) => race.complete((Errored(err), fibb).asLeft).void
      case Canceled() => fiba.cancel
    }.start
    fibrb <- fibb.join.guaranteeCase {
      case Succeeded(ioob) => ioob >>= (ob => race.complete((fiba, ob).asRight).void)
      case Errored(err) => race.complete((fiba, Errored(err)).asRight).void
      case Canceled() => fibb.cancel
    }.start
    result <- (race.get <* IO.dbg("Got result...")).onCancel {
      IO.dbg("IO-A vs IO-B race canceled!") >> (fibra.cancel, fibrb.cancel).parTupled.void
    }
  } yield result

  def ourRacePair_v2[A, B](ioa: IO[A], iob: IO[B]): IO[RaceResultIO[A, B]] = for {
    race <- IO.deferred[Either[OutcomeIO[A], OutcomeIO[B]]].onCancel(IO.void("Deferred canceled!"))
    fiba <- ioa.guaranteeCase(out => race.complete(Left(out)).void).onCancel(IO.void("IO-A canceled!")).start.onCancel(IO.void("IO-A-fiber canceled!"))
    fibb <- iob.guaranteeCase(out => race.complete(Right(out)).void).onCancel(IO.void("IO-B canceled!")).start.onCancel(IO.void("IO-B-fiber canceled!"))
    result <- race.get.onCancel(IO.void("IO-A vs IO-B race canceled!") >> (fiba.cancel, fibb.cancel).parTupled.void)
  } yield result match {
    case Left(outA) => Left(outA -> fibb)
    case Right(outB) => Right(fiba -> outB)
  }

  //def run: IO[Unit] = demoDeferred()
  //def run: IO[Unit] = fileNotifierWithRef()
  //def run: IO[Unit] = fileNotifierWithDeferred()
  //def run: IO[Unit] = alarm()
  //def run: IO[Unit] = ourRacePair(IO.sleep(250.millis) >> IO("IO-A succeeded"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.void
  //def run: IO[Unit] = ourRacePair(IO.sleep(1000.millis) >> !?("IO-A errored"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.void
  //def run: IO[Unit] = ourRacePair(IO.sleep(1000.millis) >> !?("IO-A errored"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.start >>= (fib => IO.sleep(250.millis) >> fib.cancel)
  def run: IO[Unit] = ourRacePair_v2(IO.sleep(750.millis) >> IO("IO-A succeeded"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.void
  //def run: IO[Unit] = ourRacePair_v2(IO.sleep(1000.millis) >> !?("IO-A errored"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.start >>= (fib => IO.sleep(250.millis) >> fib.cancel)
}
