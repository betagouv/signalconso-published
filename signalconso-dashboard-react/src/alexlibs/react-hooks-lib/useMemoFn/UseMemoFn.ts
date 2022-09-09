import {useMemo} from 'react'

export const useMemoFn = <T, R>(dep: T | undefined, map: (_: T) => R): R | undefined => {
  // @ts-ignore
  return useMemo(() => {
    return dep ? map(dep) : undefined
  }, [dep])
}
