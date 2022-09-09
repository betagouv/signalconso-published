// Container wrapping a value that may be undefined

// This is a very minimal implementation, with very few methods
// This class was written to replace our usages of the lib "fp-ts" which could not be upgraded
//
// In most cases you don't need this class, just use "&&", "?.", "if ()", etc.
export class ScOption<A> {
  optVal: A | undefined

  constructor(optVal: A | null | undefined) {
    this.optVal = optVal ?? undefined
  }

  static from<A>(optVal: A | null | undefined): ScOption<A> {
    return new ScOption<A>(optVal)
  }

  static none<A>(): ScOption<A> {
    return ScOption.from<A>(undefined)
  }

  /**
   * Returns this Option if it is nonempty and applying the predicate p to this Option's value returns true.
   */
  filter(f: (value: A) => boolean): ScOption<A> {
    if (this.optVal !== undefined && f(this.optVal)) {
      return this
    }
    return ScOption.none()
  }

  //   /**
  //    * Finds the first element of the iterable collection satisfying a predicate, if any.
  //    */
  //   find(p: (value: A) => boolean): Option<A> {
  //     return this.filter(p) as Option<A>
  //   }

  //   /**
  //    * Returns the result of applying f to this Option's value if this Option is nonempty.
  //    */
  //   flatMap<U>(f: (value: A) => Option<U>): Option<U> {
  //     return super.flatMap(f) as Option<U>
  //   }

  //   /**
  //    * Converts this Option of Option into an Option
  //    * e.g. some( some(1) ).flatten() -> some(1)
  //    */
  //   flatten<U>(): Option<U> {
  //     return super.flatten<U>() as Option<U>
  //   }

  //   /**
  //    * Returns true if this option is empty or the predicate p returns true when applied to this Option's value.
  //    */
  //   forall(p: (value: A) => boolean): boolean {
  //     return super.forall(p)
  //   }

  //   /**
  //    * Apply the given procedure f to the option's value, if it is nonempty.
  //    */
  //   foreach(f: (value: A) => void): void {
  //     return super.foreach(f)
  //   }

  //   get get(): A {
  //     return this.head
  //   }

  /**
   * Returns the option's value if the option is nonempty, otherwise return the result of evaluating default.
   */
  getOrElse<U>(elseVal: U): A | U {
    if (this.optVal === undefined) {
      return elseVal
    }
    return this.optVal
  }

  toUndefined(): A | undefined {
    return this.optVal
  }

  // groupBy<K>(f: (A) => K): Map<K, collection.Seq<A>>
  // Partitions this iterable collection into a map of iterable collections according to some discriminator function.

  //   /**
  //    * Optionally selects the first element.
  //    */
  //   get headOption(): Option<A> {
  //     return new Option<A>(this)
  //   }

  //   // init: collection.Seq<A>
  //   // Selects all elements except the last.

  //   // inits: collection.Iterator<collection.Seq<A>>
  //   // Iterates over the inits of this iterable collection.

  //   /**
  //    * Returns true if the option is an instance of Some, false otherwise.
  //    */
  //   get isDefined(): boolean {
  //     return !this.isEmpty
  //   }

  // iterator: Iterator<A>
  // Returns a singleton iterator returning the Option's value if it is nonempty, or an empty iterator if the option is empty.

  /**
   * Selects the last element.
   */
  //   get last(): A {
  //     if (this.isEmpty) {
  //       throw new Error('No such element: last in None')
  //     }
  //     return this.get
  //   }

  /**
   * Optionally selects the last element.
//    */
  //   get lastOption(): Option<A> {
  //     return new Option<A>(this)
  //   }

  /**
   * Returns a Some containing the result of applying f to this Option's value if this Option is nonempty.
   */
  map<U>(f: (value: A) => U): ScOption<U> {
    if (this.optVal === undefined) {
      return ScOption.none()
    }
    return ScOption.from(f(this.optVal))
  }

  flatMap<U>(f: (value: A) => U | undefined | null): ScOption<U> {
    if (this.optVal === undefined) {
      return ScOption.none()
    }
    const newVal = f(this.optVal)
    if (newVal === null || newVal === undefined) {
      return ScOption.none()
    }
    return ScOption.from(newVal)
  }

  //   /**
  //    * Returns false if the option is None, true otherwise.
  //    */
  //   get nonEmpty(): boolean {
  //     return !this.isEmpty
  //   }

  //   /**
  //    * Returns this Option if it is nonempty, otherwise return the result of evaluating alternative.
  //    */
  //   orElse(alternative: () => Option<A>): Option<A> {
  //     return this.isEmpty ? alternative() : this
  //   }

  //   /**
  //    * Returns the option's value if it is nonempty, or null if it is empty.
  //    */
  //   get orNull(): A {
  //     try {
  //       return this.get
  //     } catch (e) {
  //       return null
  //     }
  //   }

  //   /**
  //    * Returns the option's value if it is nonempty, or throws an error with the specified message
  //    */
  //   orThrow(message: () => string): A {
  //     try {
  //       return this.get
  //     } catch (e) {
  //       throw new Error(message())
  //     }
  //   }

  //   /**
  //    * Returns the option's value if it is nonempty, or undefined if it is empty.
  //    */
  //   get orUndefined(): A {
  //     try {
  //       return this.get
  //     } catch (e) {
  //       return void 0
  //     }
  //   }
}
