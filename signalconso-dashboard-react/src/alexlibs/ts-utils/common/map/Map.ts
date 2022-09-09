export type Map = {
  <A, R>(a: A | undefined, fn: (_: A) => R): R | undefined
  <A, B, R>(a: A | undefined, b: B | undefined, fn: (a: A, b: B) => R): R | undefined
  <A, B, C, R>(a: [A | undefined, B | undefined, C | undefined], fn: (a: A, b: B, c: C) => R): R | undefined
  <A, B, C, D, R>(a: [A | undefined, B | undefined, C | undefined, D | undefined], fn: (a: A, b: B, c: C, d: D) => R):
    | R
    | undefined
}
export const map: Map = (...args: any[]) => {
  const fn = args.pop()
  return args.every(_ => _ !== undefined) ? fn(...args) : undefined
}
