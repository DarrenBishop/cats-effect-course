package rtj.effectsio

import rtj.all.{*, given}

object Effects {

  // pure functional programming
  // substitution
  def combine(a: Int, b: Int): Int = a + b
  val five = combine(2, 3)
  val five_v2 = 2 + 3
  val fivr_v3 = 5

  // referential transparency = can replace an expression with its value
  // as many times as we want without changing the program's behavior
  // core tenet of functional programming

  // example: print to the console
  val printSomething: Unit = println("printing something") // returns Unit i.e. ()
  val printSomething_v2: Unit = () // not the same

  // example: change a variable
  var anInt = 0
  val changingVar: Unit = (anInt += 1) // returns Unit i.e. ()
  val changingVar_v2: Unit = () // not the same

  // side effects are inevitable for useful programs

  /*
    Effect types:
    Properties:
    - type signature describes the kind of calculation that will be performed
    - type signature describes the VALUE that will be calculated
    - when side effects are needed, effect construction is separate from effect execution (i.e. definition vs. materialization)
   */

  /*
   example: Option
   - decribes a possibly absent value
   - computes a value of type A, if it exists
   - side effects are not needed
   */
  val anOption: Option[Int] = Option(2) // Option is a type constructor

  /*
   example: Future is NOT an effect type
   - describes an asynchronous computation i.e a value that will be computed in the future
   - computes a value of type A, if it is successful
   - side effect is needed (allocating and scheduling a thread to do work), execution is NOT separate from construction
   */
  given ec: EC = EC()
  import ec.given

  val aFuture: Future[Int] = Future(42) // Future is a type constructor

  /*
   example: MyIO data type from the Monads lesson - it IS an effect type
   - describes any computation that might produce a side effect
   - calculates a value of type A, if it's successful
   - side effect is needed for the evaluation off () => A
   - the construction of MyIO does NOT produce/cause the side effect
   */
  case class MyIO[A](unsafeRun: () => A) {
    def map[B](f: A => B): MyIO[B] = MyIO(() => f(unsafeRun()))

    def flatMap[B](f: A => MyIO[B]): MyIO[B] = MyIO(() => f(unsafeRun()).unsafeRun())
  }

  val myIO: MyIO[Int] = MyIO(() => {
    println("I am the side effect")
    42
  })

  /*
   example: IO
   - describes an effectful computation i.e. a value that will be computed in the future
   - computes a value of type A, if it is successful
   - side effect is needed (allocating and scheduling a thread to do work), execution is separate from construction
   */

  /**
   * Exercises
   * 1. An IO that returns the current time of the system
   * 2. An IO that measures the duration of a computation
   * 3. An IO that prints something to the console
   * 4. An IO that reads a line (a String) from the std input
   */

  // 1
  val currentTime: MyIO[Long] = MyIO(() => System.currentTimeMillis())

  //2
  def measure[A](computation: MyIO[A]): MyIO[Long] = for
    start <- currentTime
    _ <- computation
    end <- currentTime
  yield end - start

  // equivalent to
  //def measure[A](computation: MyIO[A]): MyIO[Long] =
  //  currentTime.flatMap(st => computation.flatMap(_ => currentTime.map(et => st - et)))

  def testTimeIO(): Unit = {
    val computation = MyIO(() => Thread.sleep(2000))
    val test = measure(computation)
    println(test.unsafeRun())
  }

  //3
  def printLine(line: String): MyIO[Unit] = MyIO(() => println(line))

  //4
  def readLine(): MyIO[String] = MyIO(() => scala.io.StdIn.readLine())

  def testConsole(): Unit = {
    val program = for
      _ <- printLine("Please enter two lines:")
      line1 <- readLine()
      line2 <- readLine()
      _ <- printLine(s"$line1\n$line2")
    yield ()

    program.unsafeRun()
  }

  def main(args: Array[String]): Unit = {

    //myIO.unsafeRun()

    testTimeIO()

    testConsole()

    ec.shutdown()
  }
}
