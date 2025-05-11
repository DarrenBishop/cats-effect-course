# Deferred

## A purely functional concurrency primitive with two methods

- `get`: blocks the fiber (semantically) until a value is present
- `complete`: inserts a value that can be read by the blocked fibers

```scala 3
val aDeferred = Deferred[IO, Int]
```

### Why
- allows inter-fiber communication
- avoids busy waiting
- maintains thread safety

### Use-cases
- producer-consumer style problems
   ```scala 3
   def consumer (signal: Deferred [IO, Int]) = for {
     _ <- IO("[consumer] waiting for result...").debug
     meaningOfLife ‹- signal.get // blocks
     _ <- IO(s"[consumer] got the result: $meaningOfLife").debug
   } yield ()
   
  def producer(signal: Deferred [IO, Int]) = for {
    _ <- IO("[producer] crunching numbers...").debug
    _ <- IO.sleep(1.second)
    meaningOfLife = 42
    _ <- IO(s"[producer] complete: $meaningOfLife").debug
    _ <- signal. complete(meaningOfLife) // unblocks consumer
   } yield ()
   ```
- sending data between fibers
- notification mechanisms
