# Ref

## Why: purely functional, thread-safe state management

### Purely functional atomic reference

## Creating a `Ref` is an effectful operation

```scala 3
val atomicMol: IO[Ref[IO, Int]] = Ref[IO].of(42)
```

## Interacting with a `Ref` are effectful operations

- setting a value
   ```scala 3
   val alternateMol: IO[Unit] = atomicMol.flatMap { ref =>
     ref.set(43)
   }
   ```
- getting existing value
   ```scala 3
   val mol: IO[Int] = atomicMol.flatMap { ref =>
     ref.get
   }
   ```
- getting + setting atomically
   ```scala 3
   val getAndSetMol: IO[Int] = atomicMol.flatMap { ref =>
     ref.getAndSet(43)
   }
    ```
- updating with a function
   ```scala 3
   val fMol: IO[Unit] = atomicMol.flatMap { ref =>
     ref.update(value => value * 10)
   }
   ```
- updating + getting (old value or new value)
   ```scala 3
   val updateMol: IO[Int] = atomicMol.flatMap { ref =>
     ref.updateAndGet(value => value * 10) // get the new value
   }
   ```
- modifying + getting a derived value
  ```scala 3
   val modifiedMol = atomicMol.flatMap { ref = >
     ref.modify(value => (value * 10, s"my current goal is $value"))
   }
   ```