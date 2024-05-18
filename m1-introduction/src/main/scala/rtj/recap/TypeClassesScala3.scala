package rtj.recap

object TypeClassesScala3 {

  case class Person(name: String, age: Int)

  // type-classes

  // part 1 - type-class definition
  trait JSONSerializer[T]:
    def toJson(value: T): String

  // part 2 - type-class instances
  given stringSerializer: JSONSerializer[String] with
    def toJson(value: String): String = s""""$value""""

  given intSerializer: JSONSerializer[Int] with
    def toJson(value: Int): String = value.toString

  given personSerializer: JSONSerializer[Person] with
    def toJson(person: Person): String =
      s"""
         |{"name": "${person.name}", "age": ${person.age}}
         |""".stripMargin.trim

  // part 3 - type-class API
  def convertToJson[T](value: T)(using serializer: JSONSerializer[T]): String =
    serializer.toJson(value)

  def convertListToJson[T](list: List[T])(using serializer: JSONSerializer[T]): String =
    list.map(serializer.toJson).mkString("[", ",", "]")

  given listSerializer[T](using JSONSerializer[T]): JSONSerializer[List[T]] with
    def toJson(list: List[T]): String = convertListToJson(list)

  // part 4 - extension methods just for the types we support
  extension [T](value: T)(using serializer: JSONSerializer[T])
    def toJson: String = serializer.toJson(value)

  def main(args: Array[String]): Unit = {

    val bob = Person("Bob", 46)
    println(personSerializer.toJson(bob))
    println(convertToJson(bob))
    println(bob.toJson)

    val persons = List(Person("Alice", 23), bob)
    println(convertListToJson(persons))
    println(listSerializer.toJson(persons))
    println(persons.toJson)
  }
}
