package rtj
package recap


object ContextualAbstractionsScala2 {

  // implicit classes

  case class Person(name: String) {
    def greet(): String = s"Hi, my name is $name"
  }

  implicit class ImpersonableString(name: String) {
    def greet(): String = Person(name).greet()
  }

  // extension method
  val greeting = "Peter".greet() // new ImpersonableString("Peter").greet()

  // example: scala.concurrent.duration
  import scala.concurrent.duration._
  val oneSecond = 1.second

  // implicit arguments and values
  def increment(x: Int)(implicit amount: Int) = x + amount
  implicit val defaultAmount: Int = 10
  val twelve = increment(2) // implicit argument 10 passed by the compiler

  def multiply(x: Int)(implicit factor: Int) = x * factor
  val aHunfred = multiply(10)

  // more complex example
  trait JSONSerializer[T] {
    def toJson(value: T): String
  }

  def convertToJson[T](value: T)(implicit serializer: JSONSerializer[T]): String =
    serializer.toJson(value)

  implicit val personSerializer: JSONSerializer[Person] = new JSONSerializer[Person] {
    override def toJson(person: Person): String = s"""{"name": "${person.name}"}"""
  }

  val davidsJson = convertToJson(Person("David")) // implicit serializer passed here

  // implicit defs
  implicit def createListSerializer[T: JSONSerializer]: JSONSerializer[List[T]] = new JSONSerializer[List[T]] {
    override def toJson(value: List[T]): String = s"[${value.map(convertToJson).mkString(",")}]"
  }

  val personsJson = convertToJson(List(Person("Alice"), Person("Bob")))

  // implicit conversions (not recommended)
  case class Cat(name: String) {
    def meow(): String = s"$name is meowing"
  }

  implicit def string2Cat(name: String): Cat = Cat(name)
  val aCat: Cat = "Garfield" // string2Cat("Garfield")
  val garfieldMeowing = "Garfield".meow() // string2Cat("Garfield").meow()

  def main(args: Array[String]): Unit = {
    println(greeting)
    println(twelve)
    println(aHunfred)
    println(davidsJson)
    println(personsJson)
  }
}
