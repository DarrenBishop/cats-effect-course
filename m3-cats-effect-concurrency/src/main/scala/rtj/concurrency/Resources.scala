package rtj.concurrency

import java.io.{File, FileReader}
import java.util.Scanner

import cats.effect.{IO, IOApp}
import rtj.predef.*

object Resources extends IOApp.Simple {

  // use-case: manage a connection lifecycle
  class Connection(url: String) {
    def open(): IO[String] = IO(s"opening connection to $url").dbg
    def close(): IO[String] = IO(s"closing connection to $url").dbg
  }

  val asyncFetchUrl = for {
    fib <- new Connection("rockthejvm.com").open().andWait(Int.MaxValue.seconds).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  // problem: leaking resources

  val resourcefulAsyncFetchUrl = for {
    conn <- IO(new Connection("rockthejvm.com"))
    fib <- conn.open().andWait(Int.MaxValue.seconds).onCancel(conn.close().void).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()
  
  /*
    bracket pattern: simeIO.bracket(useResourceCb)(releaseResourceCb)
    bracket is equivalent to try-catch (but pure FP)
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
    IO(new Scanner(new FileReader(new File(path))))
  
  //def run: IO[Unit] = asyncFetchUrl
  //def run: IO[Unit] = resourcefulAsyncFetchUrl
  def run: IO[Unit] = bracketProgram
}
