import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import {useI18n} from '../../core/i18n'
import React, {useEffect} from 'react'
import {Box, Icon, InputBase, Tooltip} from '@mui/material'
import {Txt} from '../../alexlibs/mui-extension'
import {useToast} from '../../core/toast'
import {DebouncedInput} from '../../shared/DebouncedInput/DebouncedInput'
import {IconBtn} from '../../alexlibs/mui-extension'
import {useConsumerEmailValidationContext} from '../../core/context/EmailValidationContext'
import {useEffectFn} from '../../alexlibs/react-hooks-lib'
import {TrueFalseUndefined} from '../../shared/TrueFalseUndefined/TrueFalseUndefined'
import {sxUtils} from '../../core/theme'

export const ConsumerListPending = () => {
  const {m} = useI18n()
  const _users = useConsumerEmailValidationContext().search
  const _validate = useConsumerEmailValidationContext().validate
  const {toastError} = useToast()
  const {formatDate} = useI18n()

  useEffect(() => {
    _users.fetch()
  }, [])

  useEffectFn(_users.error, toastError)

  return (
    <Panel>
      <Datatable
        id="userslist"
        header={
          <>
            <DebouncedInput
              value={_users.filters.email ?? ''}
              onChange={email => _users.updateFilters(prev => ({...prev, email}))}
            >
              {(value, onChange) => (
                <InputBase
                  value={value}
                  placeholder={m.searchByEmail + '...'}
                  fullWidth
                  sx={{ml: 1}}
                  onChange={e => onChange(e.target.value)}
                />
              )}
            </DebouncedInput>
            <TrueFalseUndefined
              value={_users.filters.validated}
              onChange={_ => _users.updateFilters(prev => ({...prev, validated: _}))}
              label={{
                true: m.identified,
                false: m.notIdentified,
                undefined: m.all,
              }}
            />
          </>
        }
        loading={_users.fetching}
        total={_users.list?.totalCount}
        paginate={{
          limit: _users.filters.limit,
          offset: _users.filters.offset,
          onPaginationChange: pagination => _users.updateFilters(prev => ({...prev, ...pagination})),
        }}
        showColumnsToggle
        rowsPerPageOptions={[5, 10, 25, 100]}
        getRenderRowKey={_ => _.email}
        data={_users.list?.entities}
        columns={[
          {
            id: '',
            head: m.email,
            render: _ => <Txt bold>{_.email}</Txt>,
          },
          {
            head: m.creationDate,
            id: 'creationDate',
            render: _ => formatDate(_.creationDate),
          },
          {
            head: m.lastAttempt,
            id: 'lastAttempt',
            render: _ => formatDate(_.lastAttempt),
          },
          {
            head: m.count,
            id: 'attempts',
            render: _ => _.attempts,
          },
          {
            id: 'actions',
            head: '',
            sx: _ => sxUtils.tdActions,
            render: _ => (
              <>
                <Box sx={{display: 'flex', justifyContent: 'flex-end'}}>
                  {_.lastValidationDate !== undefined || _validate.isNowValid(_.email) ? (
                    <Tooltip title={formatDate(_.lastValidationDate ?? new Date())} placement="left">
                      <span>
                        <IconBtn disabled={true}>
                          <Icon sx={{color: t => t.palette.success.light}}>check_circle</Icon>
                        </IconBtn>
                      </span>
                    </Tooltip>
                  ) : (
                    <Tooltip title={m.validate} placement="left">
                      <IconBtn
                        color={_validate.hasError(_.email) ? 'error' : 'primary'}
                        loading={_validate.isLoading(_.email)}
                        onClick={() => _validate.call(_.email)}
                      >
                        <Icon>task_alt</Icon>
                      </IconBtn>
                    </Tooltip>
                  )}
                </Box>
              </>
            ),
          },
        ]}
      />
    </Panel>
  )
}
