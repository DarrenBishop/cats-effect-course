package rtj
package recap


object ContextualAbstractionsScala3 {

  // given/using combination
  def increment(x: Int)(using amount: Int) = x + amount
  given defaultAmount: Int = 10
  val twelve = increment(2) // implicit argument 10 passed by the compiler

  def multiply(x: Int)(using factor: Int) = x * factor
  val aHunfred = multiply(10) // implicit argument 10 passed by the compiler

  // more complex use-case
  trait Combiner[A]:
    def combine(a: A, b: A): A
    def empty: A

  def combineAll[A](values: List[A])(using combiner: Combiner[A]): A =
    values.foldLeft(combiner.empty)(combiner.combine)

  given intCombiner: Combiner[Int] with
    def combine(a: Int, b: Int): Int = a + b
    def empty: Int = 0

  val numbers = (1 to 10).toList
  val sum10 = combineAll(numbers) // intCombiner passed automatically

  // synthesize given instances
  given optionCombiner[T](using combiner: Combiner[T]): Combiner[Option[T]] with
    def empty: Option[T] = Some(combiner.empty)
    def combine(oa: Option[T], ob: Option[T]): Option[T] = for
      a <- oa
      b <- ob
    yield combiner.combine(a, b)

  val sumOption = combineAll(numbers.map(Option(_))) // optionCombiner(intCombiner) passed automatically

  // extension methods
  case class Person(name: String):
    def greet(): String = s"Hi, my name is $name"

  extension (name: String)
    def greet(): String = Person(name).greet()

  val alicesGreetings = "Alice".greet()

  // generic extension methods
  extension [T](list: List[T])
    def reduceAll(using combiner: Combiner[T]): T = combineAll(list)

  val sum10_v2 = numbers.reduceAll // intCombiner passed automatically
  val sumOption_v2 = numbers.map(Option(_)).reduceAll // intCombiner passed automatically

  def main(args: Array[String]): Unit = {

    println(twelve)
    println(aHunfred)
    println(sum10)
    println(sumOption)

    println(alicesGreetings)
    println(sum10_v2)
    println(sumOption_v2)
  }
}
