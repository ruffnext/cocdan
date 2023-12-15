type UnionToIntersection<U> = (U extends any ? (k: U) => void : never) extends (k: infer I) => void
  ? I
  : never;

type JoinPath<A, B> = A extends string | number
  ? B extends string | number
    ? `${A}.${B}`
    : A
  : B extends string | number
    ? B
    : "";

export type Flatten<T extends Object, P = {}> = number extends T
  ? /* catch any */ Object
  : T extends (infer V)[]
    ? /* array */ { [K in JoinPath<P, number>]?: V } & (V extends Object
        ? Partial<Flatten<V, JoinPath<P, number>>>
        : {})
    : /* record */ UnionToIntersection<
        { [K in keyof T]: T[K] extends Object ? Flatten<T[K], JoinPath<P, K>> : never }[keyof T]
      > & { [K in keyof T as JoinPath<P, K>]: T[K] };