package rtj.concurrency

import java.io.{File, FileReader}
import java.util.Scanner

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{IO, IOApp, Resource}
import rtj.predef.*

object Resources extends IOApp.Simple {

  /**
    * use-case: manage a connection lifecycle
    */
  class Connection(url: String) {
    def open(): IO[String] = IO.dbg(s"opening connection to $url")
    def use(): IO[Unit] = IO.dbg(s"using connection to $url").void
    def close(): IO[String] = IO.dbg(s"closing connection to $url")
  }

  val asyncFetchUrl = for {
    fib <- new Connection("rockthejvm.com").open().andWait(Int.MaxValue.seconds).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  /**
    *  problem: nesting resources are tedious, hard to read and debug
    */

  val resourcefulAsyncFetchUrl = for {
    conn <- IO(new Connection("rockthejvm.com"))
    fib <- conn.open().andWait(Int.MaxValue.seconds).onCancel(conn.close().void).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  /**
    *  bracket pattern: simeIO.bracket(useResourceCb)(releaseResourceCb)
    *  bracket is equivalent to try-catch (but pure FP)
    */

  val bracketAsyncFetchUrl = IO(new Connection("rockthejvm.com"))
    .bracket(conn => conn.open().andWait(Int.MaxValue.seconds))(conn => conn.close().void)

  val bracketProgram = for {
    fib <- bracketAsyncFetchUrl.start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  /**
    *  Exercise: read the file with the bracket pattern
    *  - open a scanner
    *  - read the file line by line, every 100 millis
    *  - close the scanner
    *  - if cancelled/throws error, close the scanner
    */
  def openFileScanner(path: String): IO[Scanner] =
    IO.dbg(s"opening file $path") *> IO(new Scanner(new FileReader(new File(path))))

  def bracketReadFile(path: String, poison: Option[String] = None): IO[Unit] =
    (openFileScanner(path).bracket { scanner =>
      IO(scanner.nextLine()).dbg
        .andWait(100.millis)
        .flatTap(line => if (poison.contains(line)) !!?(s"Poison Pill: $line!!!") else IO.unit )
        .whileM[Vector, String](IO(scanner.hasNextLine))
    } (scanner => IO.dbg(s"closing file $path") *> IO(scanner.close()))).void

  /**
    * Resources
    */
  def connFromConfig(path: String): IO[Unit] =
    openFileScanner(path)
      .bracket{ scanner =>
        // acquire a connection based on file
        IO.dbg("creating connection") *> IO(scanner.nextLine())
          .map(Connection(_))
          .bracket { conn =>
            conn.open() >> conn.use() >> IO.never
          } { conn => IO(conn.close()) }
      } { scanner => IO.dbg(s"closing file $path") *> IO(scanner. close()) }

  /**
    *  problem: nesting resources are tedious, hard to read and debug
    */

  def connectionFromR(path: String) = Resource.make(IO.dbg(s"creating connection to $path") *> IO(new Connection(path)))(_.close().void)
  val connectionResource = connectionFromR("rockthejvm.com")
  // ... then use at a later part of your code

  val resourceFetchUrl = for {
    fib <- connectionResource.use(conn => conn.open() >> conn.use() >> IO.never).start
    _ <- IO.sleep(1.second) >> fib.cancel
  } yield ()

  val simpleResource: IO[String] = IO("some resource")
  val usingResource: String => IO[String] = string => IO(s"using the string: $string").dbg
  val releaseResource: String => IO[Unit] = string => IO(s"finalizing the string: $string").dbg.void

  val usingResourceWithBracket = simpleResource.bracket(usingResource)(releaseResource)
  val usingResourceWithResource = Resource.make(simpleResource)(releaseResource).use(usingResource)

  /**
    * Exercise: read a text file with one line every 100 millis, using Resource
    * (refactor the bracket exercise to use Resource)
    */
  def resourceReadFile(path: String, poison: Option[String] = None): IO[Unit] =
    Resource
      .make(openFileScanner(path))(scanner => IO.dbg(s"closing file $path") *> IO(scanner.close()))
      .use { scanner =>
        IO(scanner.nextLine()).dbg
          .andWait(100.millis)
          .flatTap(line => if (poison.contains(line)) !!?(s"Poison Pill: $line!!!") else IO.unit )
          .whileM[Vector, String](IO(scanner.hasNextLine))
      }
      .void

  //def run: IO[Unit] = asyncFetchUrl
  //def run: IO[Unit] = resourcefulAsyncFetchUrl
  //def run: IO[Unit] = bracketProgram
  //def run: IO[Unit] = bracketReadFile(
  //  "/Users/darren/Workspaces/Courses/RockTheJVM/cats-effect-course/m3-cats-effect-concurrency/src/main/scala/rtj/concurrency/Resources.scala",
  //  Option("xxx import rtj.predef.*")
  //).dbg.silence
  //def run: IO[Unit] = resourceFetchUrl
  def run: IO[Unit] = resourceReadFile(
    "/Users/darren/Workspaces/Courses/RockTheJVM/cats-effect-course/m3-cats-effect-concurrency/src/main/scala/rtj/concurrency/Resources.scala",
    Option("xxx import rtj.predef.*")
  ).dbg.silence
}
