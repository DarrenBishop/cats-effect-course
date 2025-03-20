# Resource

## Nesting brackets is clunky

```scala 3
resource1.bracket { res1 =>
  resource2.bracket { res2 =>
    // effect here
  } (freeResource2)
} (freeResource1)
```

## Resource: a data structure (abstraction) that describes
- the effect of acquiring a resource
- the effect of releasing a resource

```scala 3
val connectionResource = Resource.make(IO(new Connection("rockthejvm.com")))(_.close().void)
```

### Use resources later, the releasing takes care of itself

```scala 3
connectionResource.use { conn => conn.open() >> someOtherEffect }
```

### Compose `Resource` with map/flatMap/for-comprehensions
- much more readable
- releasing happens in reverse order of acquisition automatically

```scala 3
for {
  scanner <- Resource.make(...)(scanner => ...)
  conn <- Resource.make(...)(conn => ...)
} yield conn
```