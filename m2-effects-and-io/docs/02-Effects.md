# Effects and IO

## Takeaways

### Pure functional program = a big expression computing a value
- referential transparency = can replace an expression with its value without changing behaviour

### Expressions performing side effects are not replaceable
- i.e. break referential transparency

### Effect = data type which
- embodies a computational concept (e.g. side effects, abscence of value)
- is referentially transparent

### Effect properties
- it describes what kind of computation it will perform
- the type signature describes the _value_ it will calculate
- it separates effect _description_ from effect _execution_ (when externally visible side effects are produced)