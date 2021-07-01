# Introduction

This is my playground with monad transformer stack in Scala3. I started it to take practices of Scala3.

## Elevator Simulation

It is a simple elevator simulation. I am not interested in elevator simulation itself, but I lined up some points.

- SCAN algorithm is taken for elevator performance.
- Instead of implementing my own discerete event scheduler, cats effect's scheduler is taken. Each passenger and each elevator holds a fiber, they run concurrently and is controlled by `Deferred`. For example, when a passenger wait on a door to get in a elevator, a fiber is suspended on a `Deferred`.

### SCAN algorithm

https://www.geeksforgeeks.org/scan-elevator-disk-scheduling-algorithms/

FCFS has poor performance, If a person want to go to 5th floor from ground floor, let's say (0, 5). In FCFS, When people came up to like (0, 5), (2, 7) and a elevator is on ground floor, a elevator travels around like 0 -> 5 -> 2 -> 7. It takes 5 + 3 + 5 = 13 ticks. so I took SCAN algorithm which takes passengers in the same direction with elevator. In SCAN, a elevator travels 0 -> 2 -> 5 -> 7, which takes 2 + 3 + 2 = 7 ticks.

## Functional Core, Imperative Shell

- https://www.destroyallsoftware.com/screencasts/catalog/functional-core-imperative-shell

I am a big fan of "Functional Core, Imperative Shell" pattern, even though "Functional Core, Functional Shell" is possible in Scala. Anyway, Specially, when I write tests, this pattern is really helpful. I think the problem of tests is on integration tests. A integration test is a scam. It is not maintainable. It should be kept as small as possible. 

All logics should resides in Core module covered by unit tests, which can run in parallel.

If Shell module does not have any logics, tests are not really required. Some simple integration test would be enough. The library developers work hard for users, I do not like to test it again.

Gary Bernhardt divides his program by values. If we have core actors and shell actors. Actors can communicate with values. Values are [Boundaries](https://www.destroyallsoftware.com/talks/boundaries). 

In scala, we can divide our program by functions, following Tagless final encoding. Functions are boundaries. We can push all side effects and states into Shell.

Core and Shell module are split it up by cats-effect for fun. so `Core` module does not depend on `cats-effect` and so on. So Additional algebra for this separation is added. For example, `FloorDoorAlg` in `Core` is created for waiting/awakening semantics over `Deferred` in `Shell`.

## No cycle dependency.

Cycle dependency is toxic. In other to avoid it, `FloorManager` And `Simulation` Algebra is introduced.

- `FloorManager`: `System` and `Elevator` talk through `FloorManager`. When a passenger's fiber wait for an elevator, the fiber wait over `FloorManager` and then the `Elevator` get arrived on the requested floor, it awake the fiber over `FloorManager`:
- `Simulation` controls simulation status. `Elevator` does not need to be aware of `System`.

<!-- This is the original graph
digraph G {
  rankdir = "LR"
  ratio = fill;
  node [style=filled];
  subgraph cluster_domain {
    rank="same"
	label = "Algebra dependency";
    "World" -> "System" [constraint=false];
    "System" -> "Elevator";   
    "Elevator" -> "FloorManager" [constraint=false];  
    "System" -> "FloorManager" [constraint=false];
    "Elevator" -> "Simulation" [constraint=false];  
    "System" -> "Simulation" [constraint=false];
  }
}
-->
![Alt text](https://g.gravizo.com/svg?digraph%20G%20%7B%0A%20%20rankdir%20%3D%20%22LR%22%0A%20%20ratio%20%3D%20fill%3B%0A%20%20node%20%5Bstyle%3Dfilled%5D%3B%0A%20%20subgraph%20cluster_domain%20%7B%0A%20%20%20%20rank%3D%22same%22%0A%09label%20%3D%20%22Algebra%20dependency%22%3B%0A%20%20%20%20%22World%22%20-%3E%20%22System%22%20%5Bconstraint%3Dfalse%5D%3B%0A%20%20%20%20%22System%22%20-%3E%20%22Elevator%22%3B%20%20%20%0A%20%20%20%20%22Elevator%22%20-%3E%20%22FloorManager%22%20%5Bconstraint%3Dfalse%5D%3B%20%20%0A%20%20%20%20%22System%22%20-%3E%20%22FloorManager%22%20%5Bconstraint%3Dfalse%5D%3B%0A%20%20%20%20%22Elevator%22%20-%3E%20%22Simulation%22%20%5Bconstraint%3Dfalse%5D%3B%20%20%0A%20%20%20%20%22System%22%20-%3E%20%22Simulation%22%20%5Bconstraint%3Dfalse%5D%3B%0A%20%20%7D%0A%7D)

## Next level MTL.

Cats' monad transformers are not a newtype, so stacked application monad like `type AppT[A] = EitherT[ReaderT[IO, AppEnv, *], AppError, A]` is not so performant. If good performance is required, surely, `type AppT[A] = ZIO[AppEnv, AppError, A]` is better until opaque type will land in monad transformers. But, I went for`cats-effect3` and `cats-mtl` this time.

`cats-mtl`'s typeclasses is promising to make a program to work over specific monad transformers. The official documentation does not guide us how to use it. In fact, we need to know something else to use `cats-mtl`:

- [Next Level MTL - George Wilson](https://www.youtube.com/watch?v=GZPup5Iuaqw)
- [Classy Optics - Gabriel Volpe](https://www.youtube.com/watch?v=gYnbOUGpWK0&t=404s)
- [Practical FP in Scala - Gabriel Volpe](https://leanpub.com/pfp-scala) book explained well about this.
- [meow-mtl](https://github.com/oleg-py/meow-mtl) provides a nice way to generate all required optics instances and mtl instances.

IMO, optics is useless in Scala. I have not seen any good use case so far except classy optics. I had ever tried optics in my project, but it is not simpler than normal `.` operator and `copy` function. Even when I have deeply nested data, I go for normal functions instead of optics.

I planned to try writing simple `meow-mtl` myself to learn Scala3's macros for fun, but I didn't complete it. For now, I used some functions instead of not optics, handmade mtl instances in scala3 syntax.

## Build

To build this project, you should publish locally kitten's dotty branch. kitten's scala3 derivation is not released yet.

