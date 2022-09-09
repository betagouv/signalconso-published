import React, {useEffect} from 'react'
import {useI18n} from '../../core/i18n'
import {Icon, InputBase, Tooltip} from '@mui/material'
import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import {DebouncedInput} from '../../shared/DebouncedInput/DebouncedInput'
import {useUnregistredWebsiteWithCompanyContext} from '../../core/context/UnregistredWebsitesContext'
import {useToast} from '../../core/toast'

import {NavLink} from 'react-router-dom'
import {siteMap} from '../../core/siteMap'
import {Btn, IconBtn} from '../../alexlibs/mui-extension'
import {ExportUnknownWebsitesPopper} from '../../shared/ExportPopper/ExportPopperBtn'
import {config} from '../../conf/config'
import {PeriodPicker} from '../../shared/PeriodPicker/PeriodPicker'
import {sxUtils} from '../../core/theme'
import {ScOption} from 'core/helper/ScOption'

export const ReportedUnknownWebsites = () => {
  const {m} = useI18n()
  const _fetch = useUnregistredWebsiteWithCompanyContext()
  const {toastError, toastSuccess} = useToast()

  useEffect(() => {
    _fetch.fetch()
  }, [])

  useEffect(() => {
    ScOption.from(_fetch.error).map(toastError)
  }, [_fetch.error])

  return (
    <Panel>
      <Datatable
        id="reportedunknownwebsites"
        header={
          <>
            <DebouncedInput
              debounce={400}
              value={_fetch.filters.q ?? ''}
              onChange={q => _fetch.updateFilters(prev => ({...prev, q: q}))}
            >
              {(value, onChange) => (
                <InputBase
                  value={value}
                  placeholder={m.searchByHost + '...'}
                  fullWidth
                  style={{minWidth: 120}}
                  sx={{ml: 1}}
                  onChange={e => onChange(e.target.value)}
                />
              )}
            </DebouncedInput>

            <PeriodPicker
              fullWidth
              value={[_fetch.filters.start, _fetch.filters.end]}
              onChange={([start, end]) => _fetch.updateFilters(prev => ({...prev, start, end}))}
            />
          </>
        }
        actions={
          <>
            <Tooltip title={m.removeAllFilters}>
              <IconBtn color="primary" onClick={_fetch.clearFilters}>
                <Icon>clear</Icon>
              </IconBtn>
            </Tooltip>

            <ExportUnknownWebsitesPopper
              disabled={ScOption.from(_fetch?.list?.totalCount)
                .map(_ => _ > config.reportsLimitForExport)
                .getOrElse(false)}
              tooltipBtnNew={ScOption.from(_fetch?.list?.totalCount)
                .map(_ => (_ > config.reportsLimitForExport ? m.cannotExportMoreReports(config.reportsLimitForExport) : ''))
                .getOrElse('')}
            >
              <IconBtn color="primary">
                <Icon>file_download</Icon>
              </IconBtn>
            </ExportUnknownWebsitesPopper>
          </>
        }
        loading={_fetch.fetching}
        total={_fetch.list?.totalCount}
        paginate={{
          limit: _fetch.filters.limit,
          offset: _fetch.filters.offset,
          onPaginationChange: pagination => _fetch.updateFilters(prev => ({...prev, ...pagination})),
        }}
        getRenderRowKey={_ => _.host}
        data={_fetch.list?.entities}
        columns={[
          {
            id: 'host',
            head: m.website,
            render: _ => <a href={'https://' + _.host}>{_.host}</a>,
          },
          {
            head: m.reports,
            id: 'reports',
            render: _ => _.count,
          },
          {
            id: 'actions',
            sx: _ => sxUtils.tdActions,
            render: _ => (
              <>
                <NavLink
                  to={siteMap.logged.reports({
                    websiteURL: _.host,
                    hasCompany: false,
                    hasForeignCountry: false,
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
  )
}
