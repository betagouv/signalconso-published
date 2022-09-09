import {Page, PageTitle} from '../../shared/Layout'
import {useI18n} from '../../core/i18n'
import {useReportsContext} from '../../core/context/ReportsContext'

import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import {alpha, Badge, Box, Button, Checkbox, Chip, Grid, Icon, Tooltip} from '@mui/material'
import {cleanObject, getHostFromUrl, textOverflowMiddleCropping} from '../../core/helper'
import React, {useCallback, useEffect, useMemo} from 'react'
import {
  mapArrayFromQuerystring,
  mapBooleanFromQueryString,
  mapDateFromQueryString,
  mapDatesToQueryString,
  useQueryString,
} from '../../core/helper/useQueryString'
import {NavLink} from 'react-router-dom'
import {SelectDepartments} from '../../shared/SelectDepartments/SelectDepartments'
import {Fender, IconBtn} from '../../alexlibs/mui-extension'
import {useToast} from '../../core/toast'
import {ReportStatusLabel} from '../../shared/ReportStatus/ReportStatus'
import {config} from '../../conf/config'
import {ReportsFilters} from './ReportsFilters'
import {siteMap} from '../../core/siteMap'
import {ExportReportsPopper} from '../../shared/ExportPopper/ExportPopperBtn'
import {EntityIcon} from '../../core/EntityIcon'
import {ScButton} from '../../shared/Button/Button'
import {Txt} from '../../alexlibs/mui-extension'
import {PeriodPicker} from '../../shared/PeriodPicker/PeriodPicker'
import {DebouncedInput} from '../../shared/DebouncedInput/DebouncedInput'
import {ReportDetailValues} from '../../shared/ReportDetailValues/ReportDetailValues'
import {styleUtils, sxUtils} from '../../core/theme'
import compose from '../../core/helper/compose'
import {Alert} from '../../alexlibs/mui-extension'
import {intersection} from '../../core/lodashNamedExport'
import {useSetState} from '../../alexlibs/react-hooks-lib'
import {DatatableToolbar} from '../../shared/Datatable/DatatableToolbar'
import {useReportContext} from '../../core/context/ReportContext'
import {Report, ReportingDateLabel, ReportTag} from '../../core/client/report/Report'
import {Id, ReportSearch} from '../../core/model'
import {ScOption} from 'core/helper/ScOption'

interface ReportSearchQs {
  readonly departments?: string[] | string
  readonly tags?: ReportTag[] | ReportTag
  readonly companyCountries?: string[] | string
  readonly siretSirenList?: string[] | string
  readonly activityCodes?: string[] | string
  start?: string
  end?: string
  email?: string
  websiteURL?: string
  phone?: string
  category?: string
  status?: string[]
  details?: string
  hasWebsite?: boolean
  hasPhone?: boolean
  hasCompany?: boolean
  hasForeignCountry?: boolean
  offset: number
  limit: number
}

export const Reports = () => {
  const {m, formatDate} = useI18n()
  const _report = useReportContext()
  const _reports = useReportsContext()
  const selectReport = useSetState<Id>()
  const {toastError} = useToast()
  const queryString = useQueryString<Partial<ReportSearch>, Partial<ReportSearchQs>>({
    toQueryString: mapDatesToQueryString,
    fromQueryString: compose(
      mapDateFromQueryString,
      mapArrayFromQuerystring(['status', 'departments', 'tags', 'companyCountries', 'siretSirenList', 'activityCodes']),
      mapBooleanFromQueryString(['hasCompany', 'hasForeignCountry', 'hasPhone', 'hasWebsite']),
    ),
  })

  useEffect(() => {
    _reports.updateFilters({..._reports.initialFilters, ...queryString.get()})
  }, [])

  useEffect(() => {
    queryString.update(cleanObject(_reports.filters))
  }, [_reports.filters])

  useEffect(() => {
    ScOption.from(_reports.error).map(toastError)
  }, [_reports.list, _reports.error])

  const getReportingDate = (report: Report) =>
    report.details.filter(_ => _.label.indexOf(ReportingDateLabel) !== -1).map(_ => _.value)

  const filtersCount = useMemo(() => {
    const {offset, limit, ...filters} = _reports.filters
    return Object.keys(cleanObject(filters)).length
  }, [_reports.filters])

  const updateFilters = useCallback((_: ReportSearch) => {
    _reports.updateFilters(prev => ({...prev, ..._}))
  }, [])

  return (
    <Page size="xl">
      <PageTitle>{m.reports_pageTitle}</PageTitle>
      {intersection([ReportTag.ReponseConso, ReportTag.Bloctel], _reports.filters.withoutTags ?? []).length !== 2 && (
        <Panel>
          <Alert
            type="info"
            action={
              <ScButton
                color="primary"
                onClick={() => _reports.updateFilters(_ => ({..._, withoutTags: [ReportTag.ReponseConso, ReportTag.Bloctel]}))}
              >
                {m.filter}
              </ScButton>
            }
          >
            <span dangerouslySetInnerHTML={{__html: m.hideAllReponseConsoAndBloctelReports}} />
          </Alert>
        </Panel>
      )}
      <Panel sx={{overflow: 'visible'}}>
        <Datatable
          id="reports"
          header={
            <>
              <Grid container spacing={1}>
                <Grid item xs={12} md={6}>
                  <DebouncedInput
                    value={_reports.filters.departments}
                    onChange={departments => _reports.updateFilters(prev => ({...prev, departments}))}
                  >
                    {(value, onChange) => (
                      <SelectDepartments label={m.departments} value={value} onChange={onChange} sx={{mr: 1}} fullWidth />
                    )}
                  </DebouncedInput>
                </Grid>
                <Grid item xs={12} md={6}>
                  <DebouncedInput<[Date | undefined, Date | undefined]>
                    value={[_reports.filters.start, _reports.filters.end]}
                    onChange={([start, end]) => {
                      _reports.updateFilters(prev => ({...prev, start, end}))
                    }}
                  >
                    {(value, onChange) => <PeriodPicker value={value} onChange={onChange} sx={{mr: 1}} fullWidth />}
                  </DebouncedInput>
                </Grid>
              </Grid>
              <DatatableToolbar
                open={selectReport.size > 0}
                onClear={selectReport.clear}
                actions={
                  <ScButton
                    loading={_report.download.loading}
                    variant="contained"
                    icon="file_download"
                    onClick={() => {
                      _report.download.fetch({}, selectReport.toArray())
                    }}
                    sx={{
                      marginLeft: 'auto',
                    }}
                  >
                    {m.download}
                  </ScButton>
                }
              >
                <span dangerouslySetInnerHTML={{__html: m.nSelected(selectReport.size)}} />
              </DatatableToolbar>
            </>
          }
          actions={
            <>
              <Tooltip title={m.removeAllFilters}>
                <Badge color="error" badgeContent={filtersCount} hidden={filtersCount === 0} overlap="circular">
                  <Button
                    color="primary"
                    onClick={_reports.clearFilters}
                    sx={{
                      minWidth: 'auto',
                      ...(filtersCount && {
                        border: t => '1px solid ' + t.palette.divider,
                        background: t => alpha(t.palette.primary.main, 0.12),
                      }),
                    }}
                  >
                    <Icon>clear</Icon>
                  </Button>
                </Badge>
              </Tooltip>
              <ExportReportsPopper
                disabled={ScOption.from(_reports?.list?.totalCount)
                  .map(_ => _ > config.reportsLimitForExport)
                  .getOrElse(false)}
                tooltipBtnNew={ScOption.from(_reports?.list?.totalCount)
                  .map(_ => (_ > config.reportsLimitForExport ? m.cannotExportMoreReports(config.reportsLimitForExport) : ''))
                  .getOrElse('')}
              >
                <IconBtn color="primary">
                  <Icon>file_download</Icon>
                </IconBtn>
              </ExportReportsPopper>
              <ReportsFilters filters={_reports.filters} updateFilters={updateFilters}>
                <Tooltip title={m.advancedFilters}>
                  <IconBtn color="primary">
                    <Icon>filter_list</Icon>
                  </IconBtn>
                </Tooltip>
              </ReportsFilters>
            </>
          }
          loading={_reports.fetching}
          paginate={{
            offset: _reports.filters.offset,
            limit: _reports.filters.limit,
            onPaginationChange: pagination => _reports.updateFilters(prev => ({...prev, ...pagination})),
          }}
          getRenderRowKey={_ => _.report.id}
          data={_reports.list?.entities}
          total={_reports.list?.totalCount}
          showColumnsToggle={true}
          columns={[
            {
              id: 'checkbox',
              head: (() => {
                const allChecked = selectReport.size === _reports.list?.entities.length
                return (
                  <Checkbox
                    disabled={_reports.fetching}
                    indeterminate={selectReport.size > 0 && !allChecked}
                    checked={allChecked}
                    onChange={() => {
                      if (allChecked) {
                        selectReport.clear()
                      } else {
                        selectReport.add(_reports.list!.entities!.map(_ => _.report.id))
                      }
                    }}
                  />
                )
              })(),
              style: {width: 0},
              render: _ => <Checkbox checked={selectReport.has(_.report.id)} onChange={() => selectReport.toggle(_.report.id)} />,
            },
            {
              id: 'companyPostalCode',
              head: m.postalCodeShort,
              sx: _ => ({
                maxWidth: 76,
              }),
              render: _ => (
                <>
                  <span>{_.report.companyAddress.postalCode?.slice(0, 2)}</span>
                  <Box component="span" sx={{color: t => t.palette.text.disabled}}>
                    {_.report.companyAddress.postalCode?.substr(2, 5)}
                  </Box>
                </>
              ),
            },
            {
              id: 'companyName',
              head: m.company,
              sx: _ => ({
                lineHeight: 1.4,
                maxWidth: 170,
              }),
              render: _ => (
                <>
                  <Box
                    component="span"
                    sx={{
                      fontWeight: 'bold',
                      marginBottom: -1,
                    }}
                  >
                    {_.report.companyName}
                  </Box>
                  <br />
                  <Box
                    component="span"
                    sx={{
                      fontSize: t => styleUtils(t).fontSize.small,
                      color: t => t.palette.text.disabled,
                    }}
                  >
                    {_.report.websiteURL ? getHostFromUrl(_.report.websiteURL) : _.report.phone ?? ''}
                  </Box>
                </>
              ),
            },
            {
              id: 'companySiret',
              head: m.siret,
              render: _ => _.report.companySiret,
            },
            {
              id: 'companyCountry',
              head: m.country,
              render: _ => _.report.companyAddress.country,
            },
            {
              id: 'category',
              head: m.problem,
              sx: _ => ({
                maxWidth: 200,
              }),
              render: _ => (
                <Tooltip
                  title={
                    <>
                      <b>{_.report.category}</b>
                      <Box component="ul" sx={{m: 0, p: 2}}>
                        {_.report.subcategories.map((s, i) => (
                          <li key={i}>{s.title}</li>
                        ))}
                      </Box>
                    </>
                  }
                >
                  <span>{_.report.category}</span>
                </Tooltip>
              ),
            },
            {
              id: 'creationDate',
              head: m.creation,
              render: _ => formatDate(_.report.creationDate),
            },
            {
              id: 'reportDate',
              head: 'Date constat',
              render: _ => getReportingDate(_.report),
            },
            {
              id: 'details',
              head: m.details,
              sx: _ => ({
                fontSize: t => styleUtils(t).fontSize.small,
                color: t => t.palette.text.secondary,
                maxWidth: 200,
                minWidth: 200,
                lineHeight: 1.4,
                whiteSpace: 'initial',
              }),
              render: _ => <ReportDetailValues input={_.report.details} lines={2} />,
            },
            {
              id: 'tags',
              head: m.tags,
              render: _ =>
                _.report.tags.map(tag => (
                  <Chip
                    key={tag}
                    size="small"
                    variant="outlined"
                    label={m.reportTagDesc[tag]}
                    sx={{
                      fontWeight: t => t.typography.fontWeightBold,
                      color: t => t.palette.text.secondary,
                    }}
                    style={{marginRight: 2}}
                  />
                )),
            },
            {
              id: 'status',
              head: m.status,
              render: _ => <ReportStatusLabel dense status={_.report.status} />,
            },
            {
              id: 'email',
              head: m.consumer,
              sx: _ => ({
                maxWidth: 160,
              }),
              render: _ => (
                <span>
                  <Box
                    component="span"
                    sx={{
                      ...(_.report.contactAgreement
                        ? {
                            color: t => t.palette.success.light,
                          }
                        : {
                            color: t => t.palette.error.main,
                          }),
                    }}
                  >
                    {textOverflowMiddleCropping(_.report.email ?? '', 25)}
                  </Box>
                  <br />
                  <Txt color="hint" size="small">
                    {_.report.consumerPhone ?? ''}
                  </Txt>
                </span>
              ),
            },
            {
              id: 'file',
              head: m.files,
              sx: _ => ({
                minWidth: 44,
                maxWidth: 100,
              }),
              render: _ =>
                _.files.length > 0 && (
                  <Badge badgeContent={_.files.length} color="primary" invisible={_.files.length === 1}>
                    <Icon sx={{color: t => t.palette.text.disabled}}>insert_drive_file</Icon>
                  </Badge>
                ),
            },
            {
              id: 'actions',
              stickyEnd: true,
              sx: _ => sxUtils.tdActions,
              render: _ => (
                <NavLink to={siteMap.logged.report(_.report.id)}>
                  <IconBtn color="primary">
                    <Icon>chevron_right</Icon>
                  </IconBtn>
                </NavLink>
              ),
            },
          ]}
          renderEmptyState={
            <Fender
              icon={EntityIcon.report}
              title={m.noReportsTitle}
              description={
                <>
                  <Txt color="hint" size="big" block gutterBottom>
                    {m.noReportsDesc}
                  </Txt>
                  <ScButton icon="clear" onClick={_reports.clearFilters} variant="contained" color="primary">
                    {m.removeAllFilters}
                  </ScButton>
                </>
              }
            />
          }
        />
      </Panel>
    </Page>
  )
}
