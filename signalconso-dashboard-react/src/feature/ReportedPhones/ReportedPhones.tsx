import React, {useEffect} from 'react'
import {Page, PageTitle} from '../../shared/Layout'
import {useReportedPhonesContext} from '../../core/context/ReportedPhonesContext'
import {useI18n} from '../../core/i18n'
import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import {NavLink} from 'react-router-dom'
import {siteMap} from '../../core/siteMap'
import {Btn, IconBtn} from '../../alexlibs/mui-extension'
import {ScInput} from '../../shared/Input/ScInput'
import {ExportPhonesPopper} from '../../shared/ExportPopper/ExportPopperBtn'

import {useToast} from '../../core/toast'
import {Icon, Tooltip} from '@mui/material'
import {PeriodPicker} from '../../shared/PeriodPicker/PeriodPicker'
import {DebouncedInput} from '../../shared/DebouncedInput/DebouncedInput'
import {Txt} from '../../alexlibs/mui-extension'
import {sxUtils} from '../../core/theme'
import {ScOption} from 'core/helper/ScOption'

export const ReportedPhones = () => {
  const _reportedPhone = useReportedPhonesContext()
  const {m} = useI18n()
  const {toastError} = useToast()

  useEffect(() => {
    _reportedPhone.fetch()
  }, [])

  useEffect(() => {
    ScOption.from(_reportedPhone.error).map(toastError)
  }, [_reportedPhone.error])

  return (
    <Page>
      <PageTitle>{m.reportedPhoneTitle}</PageTitle>
      <Panel>
        <Datatable
          id="reportedphones"
          showColumnsToggle
          header={
            <>
              <DebouncedInput
                value={_reportedPhone.filters.phone ?? ''}
                onChange={phone => _reportedPhone.updateFilters(prev => ({...prev, phone}))}
              >
                {(value, onChange) => (
                  <ScInput
                    style={{minWidth: 120}}
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    fullWidth
                    sx={{mr: 1}}
                    label={m.phone}
                  />
                )}
              </DebouncedInput>
              <DebouncedInput<[Date | undefined, Date | undefined]>
                value={[_reportedPhone.filters.start, _reportedPhone.filters.end]}
                onChange={([start, end]) => _reportedPhone.updateFilters(prev => ({...prev, start, end}))}
              >
                {(value, onChange) => <PeriodPicker value={value} onChange={onChange} fullWidth />}
              </DebouncedInput>
            </>
          }
          actions={
            <>
              <Tooltip title={m.removeAllFilters}>
                <IconBtn color="primary" onClick={_reportedPhone.clearFilters}>
                  <Icon>clear</Icon>
                </IconBtn>
              </Tooltip>
              <ExportPhonesPopper>
                <IconBtn color="primary">
                  <Icon>file_download</Icon>
                </IconBtn>
              </ExportPhonesPopper>
            </>
          }
          sort={{
            sortBy: _reportedPhone.filters.sortBy!,
            orderBy: _reportedPhone.filters.orderBy!,
            onSortChange: (sort: any) => _reportedPhone.updateFilters(prev => ({...prev, ...sort})),
          }}
          paginate={{
            onPaginationChange: pagination => _reportedPhone.updateFilters(prev => ({...prev, ...pagination})),
            offset: _reportedPhone.filters.offset,
            limit: _reportedPhone.filters.limit,
          }}
          total={_reportedPhone.list?.totalCount}
          loading={_reportedPhone.fetching}
          data={_reportedPhone.list?.entities}
          columns={[
            {
              id: 'phone',
              head: m.phone,
              render: _ => _.phone,
            },
            {
              id: 'category',
              head: m.category,
              render: _ => _.category,
            },
            {
              id: 'siret',
              head: m.siret,
              sx: _ => ({
                maxWidth: 200,
              }),
              render: _ => (
                <>
                  <Txt bold>{_.siret}</Txt>
                  <br />
                  <Txt color="hint">{_.companyName}</Txt>
                </>
              ),
            },
            {
              id: 'count',
              head: m.reportsCount,
              render: _ => _.count,
            },
            {
              id: 'actions',
              sx: _ => sxUtils.tdActions,
              render: _ => (
                <>
                  <NavLink
                    to={siteMap.logged.reports({
                      phone: _.phone,
                      ...(_.siret ? {siretSirenList: [_.siret]} : {}),
                      ...(_.category ? {category: _.category} : {}),
                    })}
                  >
                    <Btn size="small" color="primary" variant="outlined">
                      {m.see}
                    </Btn>
                  </NavLink>
                </>
              ),
            },
          ]}
        />
      </Panel>
    </Page>
  )
}
