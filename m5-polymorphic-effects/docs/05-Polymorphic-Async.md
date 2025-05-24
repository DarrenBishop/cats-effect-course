# Polymorphic Asynchronous

## Async = the ability to suspend asynchronous effects from outside Cats Effect

- `delay` = wrapping any computation in `F`
- `blocking` = a semantically blocking computation, wrapped in `F`

```scala 3 mdoc:invisible
import cats.Defer
import cats.effect.MonadCancel
```

```scala 3 mdoc
trait Async[F[_]] extends Sync[F] with Temporal[F] {
  def executionContext: F[ExecutionContext]
  def async[A](cb: (Either[Throwable, A] => Unit) => F[Option[F[Unit]]]): F[A]
  def evalOn[A](fa: F[A], ec: ExecutionContext): F[A]

  def async_[A](cb: (Either[Throwable, A] => Unit) => Unit): F[A] =
    async { cb_ => as(delay(cb(cb_)), None) }
  // never-ending effect
  def never[A]: F[A] =
    async(_ => pure(None))
}
```

### Goal: generalize asynchronous code for any effect type
- Foreign Function Interface (FFI): suspending computations (including side-effects) executed in another context
- Most powerful type-class in Cats-Effect

# Type Class Hierarchy

```mermaid
flowchart BT
    F("`**Functor**
    _map_`")
    AP("`**Apply**
    _ap_`")
    AP --> F
    FM("`**FlatMap**
    _flatMap_`")
    FM --> AP
    A("`**Applicative**
    _pure_`")
    A --> AP
    AE("`**ApplicativeError**
    _raiseError_
    _handleErrorWith_`")
    AE --> A
    M("`**Monad**`")
    M --> FM
    M --> A
    ME("`**MonadError**
    _ensure_`")
    ME --> M
    ME ---> AE
    D("`**Defer**
    _defer_`")
    MC("`**MonadCancel**
    _uncancelable_
    _canceled_
    _onCancel_
    _guarantee_
    _guaranteeCase_
    _bracket_`")
    MC --> ME
    S("`**Spawn**
    _start_
    _never_
    _cede_
    _racePair_`")
    S --> MC
    C("`**Concurrent**
    _ref_
    _deferred_`")
    C --> S
    T("`**Temporal**
    _sleep_
    _timeout_`")
    T --> C
    SY("`**Sync**
    _delay_
    _blocking_`")
    SY --> MC
    SY ---> D
    AY("`**Async**
    _executionContext_
    _async_
    _evalOn_
    _async__
    _never_`")
    AY --> SY
    AY --> T
    classDef CATS fill:#afd19f,stroke:#96b389,stroke-width:1px,color:black
    classDef CE fill:#f4b083,stroke:#d1b39f,stroke-width:1px,color:black
    
    class F,A,FM,AP,M,AE,ME,D CATS
    class MC,S,C,T,SY,AY CE
```
