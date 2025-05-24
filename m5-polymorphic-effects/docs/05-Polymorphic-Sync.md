# Polymorphic Synchronous

## Sync = the ability to suspend effects synchronously

- `delay` = wrapping any computation in `F`
- `blocking` = a semantically blocking computation, wrapped in `F`

```scala 3 mdoc:invisible
import cats.Defer
import cats.effect.MonadCancel
```

```scala 3 mdoc
trait Sync[F[_]] extends MonadCancel[F, Throwable] with Defer[F] {
  def delay[A](thunk: => A): F[A] // "suspension" of a computation - will run on the CE thread pool
  def blocking[A](thunk: => A): F[A] // runs on the blocking thread pool

  // defer comes for free
  def defer[A](fa: => F[A]): F[A] = flatten(delay(fa))
}
```

### Goal: generalize synchronous code for any effect type
- Foreign Function Interface (FFI): suspending computations (including side-effects) executed in another context

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
    ME --> AE
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
    
    classDef CATS fill:#afd19f,stroke:#96b389,stroke-width:1px,color:black
    classDef CE fill:#f4b083,stroke:#d1b39f,stroke-width:1px,color:black
    
    class F,A,FM,AP,M,AE,ME,D CATS
    class MC,S,C,T,SY CE
```
