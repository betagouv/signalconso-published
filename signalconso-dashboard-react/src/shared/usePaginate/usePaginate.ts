import {ISearch, UsePaginate, usePaginate} from '../../alexlibs/react-hooks-lib'
import {Paginate as ApiPaginate} from '../../core/model'

export const useScPaginate = <T, S extends ISearch<any>, E = any>(
  fetcher: (search: S) => Promise<ApiPaginate<T>>,
  initialFilters: S,
  mapError?: (_: any) => E,
): UsePaginate<T, S, E> => {
  return usePaginate(fetcher, initialFilters, mapError)
}
