import { Effect, Layer, ManagedRuntime } from "effect"
import * as ServiceMap from "effect/ServiceMap"

export const memoMap = Layer.makeMemoMapUnsafe()

// Accept layers whose RIn may not fully resolve to `never` due to
// TypeScript inference limitations with chained Layer.provide calls
// in Effect 4.x beta.  At runtime the providers are present; the cast
// inside ManagedRuntime.make is safe.
export function makeRunPromise<I, S, E>(
  service: ServiceMap.Service<I, S>,
  layer: Layer.Layer<I, E, any>,
) {
  let rt: ManagedRuntime.ManagedRuntime<I, E> | undefined

  return <A, Err>(fn: (svc: S) => Effect.Effect<A, Err, I>, options?: Effect.RunOptions) => {
    rt ??= ManagedRuntime.make(layer as Layer.Layer<I, E>, { memoMap })
    return rt.runPromise(service.use(fn), options)
  }
}
