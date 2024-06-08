## Effects

```scala 3 mdoc:invisible
import rtj.all.{*, given}
```

Descriptions of computations to be performed at our discretion

### A bridge between
 - pure functional programming & referential transparency
 - impure functional/imperative programming & side effects

### Effect properties
 - it describes what kind of computation will be performed
 - the type signature describes the _value_ that will be created
 - it separates effect description from effect execution (when
    externally visible side effects are produced)

## Cats Effect IO

### The ultimate effect type
 - any computation that might perform side effects
 - produces a value of type A if it's successful
 - the effect construction is separate from teh effect execution
```scala 3 mdoc
val ourFirstIO: IO[Int] = IO.pure(42) // must not have side-effects
val aDelayedIO: IO[Int] = IO.delay { // computation is not performed
  println("I'm producing an integer")
  54
}
```

```scala 3
val aDelayedIO_v2: IO[Int] = IO { ??? } // apply == delay
 ```

Expressions and methods returning IOs are called _effectful!_

Perform the effects at the _"end of the world"

```scala 3 mdoc
import cats.effect.unsafe.implicits.global
myBigEffect.unsafeRunSync()
```

### IO transformations: map, flatMap
```scala 3 mdoc
val improvedMeaningOfLife = ourFirstIO.map(_ * 2)
val printedMeaningOfLife = ourFirstIo.flatMap(mol => IO.delay(println(mol)))

def smallProgram(): IO[Unit] = for {
  line1 <- IO(StdIn.readLine())
  line2 <- IO(StdIn.readLine())
  _ <- IO.delay(println(s"$line1 $line2"))
} yield ()
```

### IO compositions read like an imperative program
 - pure FP is preserved

### IO is a monad

### Other transformation & syntax
 - `>>`, `*>`, `<*`
 - `.as(...)`, `.void`

### Creating failed effects

```scala 3 mdoc
val aFailedCompute: IO[Int] = IO.delay(throw new RuntimeException("A FAILURE"))
val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper fail"))
```

### Handling errors

```scala 3 mdoc
val dealWithIt = aFailure.handleErrorWith {
  case _: RuntimeException => IO.delay(println("I'm still here"))
}
```

### Transform IOs to also hold failures to process later

```scala 3 mdoc
val effectAsEither: IO[Either[Throwable, Int]] = aFailure.attempt
```

### IO parallelism
 - effects are evaluated on different threads
 - synchronisation and coordination are automatic

```scala 3 mdoc
val meaningOfLife: IO[Int] = IO.delay(42)
val favLang: IO[String] = IO.delay("Scala")

import cats.syntax.parallel.*
val goalInLifeParallel: IO[String] = (meaningOfLife.dbg, favLang,dbg).parMapN(_ + _)
```

### IO traversal
 - useful when we want to "unwrap" double-nested containers
 - can be done in parallel

```scala 3 mdoc
val workload: List[String] = List("I quite like CE", "Scala is great", "looking forward to some awesome stuff")
def computeAsIO(string: String): IO[Int] = ???

import cats.instances.list._ // Traverse type-class instance for List
import cats.syntax.parallel._ // parTraverse extension method

val parallelSingleIO: IO[List[Int]] = workload.parTraverse(computeAsIO)
```