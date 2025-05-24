# Polymorphic Coordination

## Concurrent = the ability to create concurrency primitives

- `Ref` and `Deferred` are the basic primitives
- Can create everything else in terms of them (e.g. `Mutex`, `CyclicBarrier`, `CountDownLatch`, etc)

```scala 3 mdoc:invisible
import rtj.all.{*, given}
import cats.effect.{Deferred, Ref, Spawn}
```

```scala 3 mdoc
trait Concurrent[F[_]] extends Spawn[F] {
  def ref[A](a: A): F[Ref[F, A]]
  def deferred[A]: F[Deferred[F, A]]
}
```

### Goal: generalize concurrent code for any effect type

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
    
    classDef CATS fill:#afd19f,stroke:#96b389,stroke-width:1px,color:black
    classDef CE fill:#f4b083,stroke:#d1b39f,stroke-width:1px,color:black
    
    class F,A,FM,AP,M,AE,ME CATS
    class MC,S,C CE
```
