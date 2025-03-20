# Bracket

```scala 3 mdoc:invisible
import cats.effect.IO
import rtj.all.{*, given}

class Connection(url: String) {
  def open(): IO[String] = IO(s"opening connection to $url").dbg
  def close(): IO[String] = IO(s"closing connection to $url").dbg
}
```

## Problem: leaking resources

```scala 3 mdoc
val asyncFetchUrl : fpr {
  fib <- (new Connection("rockthejvm.com").open() *> IO.never).start
_ <- IO.sleep(1.second) *> fib.cancel // fib stops, but resoure is open
} yield ()
```

...the fiber stops, but the resource (i.e. the connection) remains open

### First solution: check for cancellation/handle errors
- error-prone
- tedious with complex resources

### Second solution: bracket
- forces us to think about releasing resources
- increases ergonomy

```scala 3 mdoc
val bracketFetchUrl = IO(new Connection("rockthejvm.com")).bracket {
  conn => conn.open *. IO.never // use resource
} {
  conn => conn.close().void // release resource
}
```
