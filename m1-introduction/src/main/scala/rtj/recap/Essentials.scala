package rtj
package recap

import scala.util.{Failure, Success, Try}


object Essentials {

  // values
  val aBoolean: Boolean = false // immutable

  // expressions are EVALUATED to a value
  val anIfExpression = if (2 > 3) "bigger" else "smaller"

  // instructions vs expressions
  val theUnit = println("Hello, Scala") // Unit = void in other languages

  // OOP
  class Animal
  class Cat extends Animal
  trait Carnivore {
    def eat(animal: Animal): Unit
  }

  // inheritance model: extends <= 1 class, but inherits >= 0 traits
  class Crocodile extends Animal with Carnivore {
    override def eat(animal: Animal): Unit = println("Crunch!")
  }

  // singleton
  object MySingleton // singleton patter in one line

  // companions
  object Carnivore // companion object of the trait Carnivore

  // generics
  class MyList[A]

  // method notation
  val three = 1 + 2
  val anotherThree = 1.+(2)

  // functional programming
  val incrementer: Int => Int = x => x + 1
  val incremented = incrementer(45) // 46

  // map, flatMap, filterm
  val processedList = List(1, 2, 3).map(incrementer) // List(2, 3, 4)
  val aLongerList = List(1, 2, 3).flatMap(x => List(x, x + 1)) // List(1, 2, 2, 3, 3, 4)

  // for-comprehensions
  val checkerboard = List(1, 2, 3).flatMap(n => List('a', 'b', 'c').map(c => (n, c)))
  val anotherCheckerboard = for {
    n <- List(1, 2, 3)
    c <- List('a', 'b', 'c')
  } yield (n, c) // equivalent expression

  // options and try
  val anOption: Option[Int] = Option(/* something that might be null */ 3) // Some(3) // Something that may have a value or not
  val doubleOption: Option[Int] = anOption.map(_ * 2) // Some(6)

  val anAttempt = Try(/* something that might throw */ 42) // Success(42)
  val aModifiedAttempt = anAttempt.map(_ + 10) // Success(52)

  // pattern matching
  val anUnknown = 45
  val ordinal = anUnknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  val anOptionDescription: String = anOption match {
    case Some(value) => s"the option is not empty: $value"
    case None => "the option is empty"
  }

  // futures
  //import scala.concurrent.ExecutionContext.Implicits.global // no good due to insufficient nested future concurrency defaults
  //implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  implicit val ec: EC = EC()

  val aFuture = Future {
    // a bit of code
    43
  }

  // wait for completion (async)
  aFuture.onComplete {
    case Success(value) => println(s"The async meaning of life is $value")
    case Failure(exception) => println(s"Meaning of value failed: $exception")
  }

  // map a future
  val anotherFuture = aFuture.map(_ + 1) // Future(43) when it completes

  // partial functions
  val aPartialFunction: PartialFunction[Int, Int] = {
    case 1 => 43
    case 8 => 56
    case 100 => 999
  }

  // some more advanced stuff
  trait HigherKindedType[F[_]]
  trait SequenceChecker[F[_]] {
    def isSequential: Boolean
  }

  val listChecker = new SequenceChecker[List] {
    override def isSequential: Boolean = true
  }

  def main(args: Array[String]): Unit = {

    ec.shutdown()
  }
}
