import {
  Box,
  LinearProgress,
  SxProps,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  Theme,
} from '@mui/material'
import React, {CSSProperties, ReactNode, useMemo} from 'react'
import {DatatableColumnToggle} from './DatatableColumnsToggle'
import {useI18n} from '../../core/i18n'
import {Fender} from '../../alexlibs/mui-extension'
import {usePersistentState} from '../../alexlibs/react-persistent-state'
import {combineSx, sxUtils} from '../../core/theme'

type OrderBy = 'asc' | 'desc'

export interface DatatableProps<T> {
  id?: string
  header?: ReactNode
  actions?: ReactNode
  loading?: boolean
  total?: number
  data?: T[]
  getRenderRowKey?: (_: T) => string
  onClickRows?: (_: T, event: React.MouseEvent<HTMLTableRowElement>) => void
  columns: DatatableColumnProps<T>[]
  showColumnsToggle?: boolean
  showColumnsToggleBtnTooltip?: string
  renderEmptyState?: ReactNode
  rowsPerPageOptions?: number[]
  paginate?: {
    minRowsBeforeDisplay?: number
    offset: number
    limit: number
    onPaginationChange: (_: {offset?: number; limit?: number}) => void
  }
  sort?: {
    sortableColumns?: string[]
    sortBy?: string
    orderBy?: OrderBy
    onSortChange: (_: {sortBy?: string; orderBy?: OrderBy}) => void
  }
}

export interface DatatableColumnProps<T> {
  id: string
  head?: string | ReactNode
  render: (_: T) => ReactNode
  hidden?: boolean
  alwaysVisible?: boolean
  className?: string | ((_: T) => string | undefined)
  sx?: (_: T) => SxProps<Theme> | undefined
  style?: CSSProperties
  stickyEnd?: boolean
}

const safeParseInt = (maybeInt: any, defaultValue: number): number => (isNaN(maybeInt) ? defaultValue : parseInt(maybeInt))

const sxStickyEnd: SxProps<Theme> = {
  paddingTop: '1px',
  position: 'sticky',
  right: 0,
  background: t => t.palette.background.paper,
}

export const Datatable = <T extends any = any>({
  id,
  loading,
  total,
  data,
  columns,
  getRenderRowKey,
  actions,
  header,
  showColumnsToggle,
  showColumnsToggleBtnTooltip,
  renderEmptyState,
  rowsPerPageOptions = [5, 10, 25, 100],
  sort,
  onClickRows,
  paginate,
}: DatatableProps<T>) => {
  const {m} = useI18n()
  const displayableColumns = useMemo(() => columns.filter(_ => !_.hidden), [columns])
  const toggleableColumnsName = useMemo(
    () => displayableColumns.filter(_ => !_.alwaysVisible && _.head && _.head !== ''),
    [displayableColumns],
  )
  const [hiddenColumns, setHiddenColumns] = usePersistentState<string[]>([], id)
  const filteredColumns = useMemo(() => displayableColumns.filter(_ => !hiddenColumns.includes(_.id)), [columns, hiddenColumns])
  const displayTableHeader = useMemo(() => !!displayableColumns.find(_ => _.head !== ''), [displayableColumns])

  return (
    <>
      {(header || showColumnsToggle) && (
        <Box
          sx={{
            position: 'relative',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            minHeight: 52,
            borderBottom: t => `1px solid ${t.palette.divider}`,
            pl: 1,
            pr: 1,
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              flex: 1,
            }}
          >
            {header}
          </Box>
          <Box
            sx={{
              ml: 1,
              whiteSpace: 'nowrap',
            }}
          >
            {actions}
            {showColumnsToggle && (
              <DatatableColumnToggle
                style={{marginLeft: 'auto'}}
                columns={toggleableColumnsName}
                hiddenColumns={hiddenColumns}
                onChange={_ => setHiddenColumns(_)}
                title={showColumnsToggleBtnTooltip ?? m.toggleDatatableColumns}
              />
            )}
          </Box>
        </Box>
      )}
      <Box sx={{overflowX: 'auto', position: 'relative'}} id={id}>
        <Table
          sx={{
            minWidth: '100%',
            tableLayout: 'fixed',
            width: 'auto', // Override width: 100% from Material-UI that breaks sticky columns
          }}
        >
          {displayTableHeader && (
            <TableHead>
              <TableRow>
                {filteredColumns.map((_, i) => (
                  <TableCell
                    key={i}
                    sx={{
                      color: t => t.palette.text.secondary,
                      ...(_.stickyEnd && sxStickyEnd),
                    }}
                  >
                    {sort && (sort.sortableColumns?.includes(_.id) ?? true) ? (
                      <TableSortLabel
                        sx={{color: t => t.palette.primary.main}}
                        active={sort.sortBy === _.id}
                        direction={sort.sortBy === _.id ? sort.orderBy : 'asc'}
                        onClick={() => {
                          if (sort.sortBy === _.id && sort.orderBy === 'desc') {
                            sort.onSortChange({sortBy: undefined, orderBy: undefined})
                          } else {
                            sort.onSortChange({sortBy: _.id, orderBy: sort.orderBy === 'asc' ? 'desc' : 'asc'})
                          }
                        }}
                      >
                        {_.head}
                      </TableSortLabel>
                    ) : (
                      _.head
                    )}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
          )}
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell
                  colSpan={filteredColumns.length}
                  sx={{
                    height: 0,
                    marginBottom: '-1px',
                    padding: 0,
                    border: 'none',
                  }}
                >
                  <LinearProgress />
                </TableCell>
              </TableRow>
            )}
            {data?.map((item, i) => (
              <TableRow
                key={getRenderRowKey ? getRenderRowKey(item) : i}
                onClick={e => onClickRows?.(item, e)}
                sx={{
                  ...(onClickRows && {
                    '&:hover': {
                      background: t => t.palette.action.hover,
                    },
                  }),
                }}
              >
                {filteredColumns.map((_, i) => (
                  <TableCell
                    key={i}
                    sx={combineSx(_.sx?.(item), sxUtils.truncate, sxStickyEnd)}
                    style={_.style}
                    className={typeof _.className === 'function' ? _.className(item) : _.className}
                  >
                    {_.render(item)}
                  </TableCell>
                ))}
              </TableRow>
            ))}
            {!loading && (!data || data?.length === 0) && (
              <TableRow>
                <TableCell colSpan={filteredColumns.length} sx={{p: 2, textAlign: 'center'}}>
                  {renderEmptyState ? renderEmptyState : <Fender title={m.noDataAtm} icon="highlight_off" />}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Box>
      {paginate && total && (!paginate.minRowsBeforeDisplay || total > paginate.minRowsBeforeDisplay)
        ? (() => {
            const limit = safeParseInt(paginate.limit, 10)
            const offset = safeParseInt(paginate.offset, 0)
            return (
              <TablePagination
                rowsPerPageOptions={rowsPerPageOptions}
                component="div"
                count={total ?? 0}
                rowsPerPage={limit}
                page={offset / limit}
                onPageChange={(event: unknown, newPage: number) => paginate.onPaginationChange({offset: newPage * limit})}
                onRowsPerPageChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  paginate.onPaginationChange({limit: +event.target.value})
                }
              />
            )
          })()
        : data && (
            <Box
              sx={{
                py: 0,
                px: 2,
                minHeight: 52,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'flex-end',
              }}
            >
              <span dangerouslySetInnerHTML={{__html: m.nLines(data.length)}} />
            </Box>
          )}
    </>
  )
}
