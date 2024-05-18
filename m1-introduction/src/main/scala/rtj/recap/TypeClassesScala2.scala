package rtj
package recap


object TypeClassesScala2 {
  case class Person(name: String, age: Int)

  // part 1 - typeclass definition
  trait JSONSerializer[T] {
    def toJson(value: T): String
  }

  // part 2 - implicit typeclass instances
  implicit object StringSerializer extends JSONSerializer[String] {
    override def toJson(value: String): String = s""""$value""""
  }

  implicit object IntSerializer extends JSONSerializer[Int] {
    override def toJson(value: Int): String = value.toString
  }

  implicit object PersonSerializer extends JSONSerializer[Person] {
    override def toJson(value: Person): String = {
      s"""
         |{"name": "${value.name}", "age": ${value.age}}
         |""".stripMargin.trim
    }
  }

  implicit def listSerializer[T](implicit serializer: JSONSerializer[T]): JSONSerializer[List[T]] = new JSONSerializer[List[T]] {
    override def toJson(value: List[T]): String = value.map(serializer.toJson).mkString("[", ",", "]")
  }

  // part 3 - api
  def convertToJson[T](value: T)(implicit serializer: JSONSerializer[T]): String =
    serializer.toJson(value)

  def convertListToJson[T](ts: List[T])(implicit serializer: JSONSerializer[T]): String =
    ts.map(serializer.toJson).mkString("[", ",", "]")

  // part 4 - extension methods
  object JSONSyntax {
    implicit class JSONSerializable[T](value: T)(implicit serializer: JSONSerializer[T]) {
      def toJson: String = serializer.toJson(value)
    }
  }

  def main(args: Array[String]): Unit = {
    val person = Person("Alice", 29)
    val persons = List(Person("Alice", 29), Person("Bob", 30))

    println(convertToJson("Hello, Typeclasses"))
    println(convertToJson(42))
    println(convertToJson(person))
    println(convertListToJson(persons))

    import JSONSyntax.*
    println("Hello, Typeclasses".toJson)
    println(42.toJson)
    println(person.toJson)
    println(persons.toJson)
  }
}