package rtj.concurrency

import cats.effect.{IO, IOApp}
import rtj.predef.*

object CancellingIOs extends IOApp.Simple {

  /**
    * Cancelling IOs
    * - fib.cancel
    * - IO.race & other APIs
    * - manual cancellation
    */
  val chainOfIOs: IO[Int] = IO.dbg("waiting") >> IO.canceled >> IO(42).dbg

  /**
    * Example: uncancelable
    *
    * - Online payment processor; payment process must NOT be canceled
    */
  val specialPaymentSystem = {
    IO.dbg("Payment running; do not cancel!") >>
      IO.sleep(1000.millis) >>
        IO.dbg("Payment complete.")
  }.onCancel(IO.dbg("MEGA CANCEL OF DOOM!").void)

  val cancellationOfDoom = for {
    fib <- specialPaymentSystem.start
    _ <- IO.sleep(500.millis) *> fib.cancel
    _ <- fib.join
  } yield ()

  val atomicPayment = IO.uncancelable((_ => specialPaymentSystem)) // "masking"
  val atomicPayment_v2 = specialPaymentSystem.uncancelable // equivalent

  val noCancellationOfDoom = for {
    fib <- atomicPayment_v2.start
    _ <- IO.sleep(500.millis) >> IO.dbg("attempting cancellation...") >> fib.cancel
    _ <- fib.join
  } yield ()

  /**
    * The uncancelable API is more complex and more general.
    * It takes a function from Poll[IO] to IO. In the example above, we aren't using that Poll instance.
    * The Poll object can be used to mark sections within the returned effect which CAN BE CANCELED.
    */

  /**
    * Example: authentication service has two parts:
    * - input password, can be cancelled, because otherwise we might block indefinitely on user input
    * - verify password, CANNOT be cancelled once it's started
    */
  val inputPassword: IO[String] =
    IO.dbg("Input password") >>
      IO.dbg("(typing password)") >>
        IO.sleep(2000.millis) >>
          IO("RockTheJVM123!")

  val verifyPassword: String => IO[Boolean] = { (password: String) =>
    IO.dbg("verifying...") >>
      IO.sleep(2000.millis) >>
        IO(password == "RockTheJVM123!")
  }

  val authFlow: IO[Unit] = IO.uncancelable { poll =>
    for {
      pw <- poll(inputPassword).onCancel(IO.void("Authentication timed out. Try again later!")) // this is cancelable
      verified <- verifyPassword(pw) // this is NOT cancelable
      _ <- if (verified) IO.dbg("Authentication successful.") else IO.dbg("Authentication failed!") // this is NOT cancelable
    } yield ()
  }

  val authProgram = for {
    authFib <- authFlow.start
    _ <- IO.sleep(3.seconds) >> IO.dbg("Authentication timeout, attempting cancel...") >> authFib.cancel
    _ <- authFib.join
  } yield ()

  /**
    * Uncancelable calls are MASKS which supress cancellation.
    *
    * Poll calls are "gaps opened" in the uncancelable region
    */

  def run: IO[Unit] = authProgram
}
