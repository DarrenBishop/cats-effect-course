# Polymorphic Temporal

## Temporal = the ability to create time-limited blocking effects

- Fundamental operation: `sleep`
- Extra functionality unlocked: `timeout`

```scala 3 mdoc:invisible
import cats.effect.Concurrent
import scala.concurrent.duration.FiniteDuration
```

```scala 3 mdoc
trait Temporal[F[_]] extends Concurrent[F] {
  def sleep(time: FiniteDuration): F[Unit] // semantically blocks this fiber for a specified time
}
```

### Goal: generalize time-based code for any effect type

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
    classDef CATS fill:#afd19f,stroke:#96b389,stroke-width:1px,color:black
    classDef CE fill:#f4b083,stroke:#d1b39f,stroke-width:1px,color:black
    
    class F,A,FM,AP,M,AE,ME CATS
    class MC,S,C,T CE
```
