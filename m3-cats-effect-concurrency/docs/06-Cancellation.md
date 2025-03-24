# Cancellation

```scala 3 mdoc:invisible
import cats.effect.IO
import rtj.all.{*, given}

val specialPaymentSystme = IO.dbg("making payment ")
```

## Cancellation Regions

### Make effects ignore cancellation signals with uncancelable

```scala mdoc
val atomicPayment = IO.cancelable(_ => specialPaymentSystme)
```

### Define pinpoint cancellation regions by using the poll
- everything wrapped in the poll call is cancelable
- everything else is _**not**_ cancelable

> ```scala mdoc
> val authFlow: IO[Unit] = IO.uncancelable { poll =>                                                // uncancelable
>   for {                                                                                           // uncancelable
>     pw <- poll(inputPassword).onCancel(IO.void("Authentication timed out. Try again later!"))     // cancelable
>     verified <- verifyPassword(pw)                                                                // uncancelable
>     _ <- if (verified) IO.dbg("Authentication successful.") else IO.dbg("Authentication failed!") // uncancelable
>   } yield ()                                                                                      // uncancelable
> }                                                                                                 // uncancelable
> ```

### The poll is the local opposite of uncancelable
- can be called as many times as we like
