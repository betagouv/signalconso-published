import React, {useEffect, useMemo} from 'react'
import {Page, PageTitle} from '../../shared/Layout'
import {Panel, PanelBody} from '../../shared/Panel'
import {useReportsContext} from '../../core/context/ReportsContext'
import {Datatable} from '../../shared/Datatable/Datatable'
import {useI18n} from '../../core/i18n'
import {Badge, Box, Grid, Icon, MenuItem} from '@mui/material'
import {ReportStatusLabel, ReportStatusProLabel} from '../../shared/ReportStatus/ReportStatus'
import {useLayoutContext} from '../../core/Layout/LayoutContext'
import {Txt} from '../../alexlibs/mui-extension'

import {combineSx, styleUtils, sxUtils} from '../../core/theme'
import {SelectDepartments} from '../../shared/SelectDepartments/SelectDepartments'
import {ScSelect} from '../../shared/Select/Select'
import {useHistory} from 'react-router'
import {siteMap} from '../../core/siteMap'
import {Btn, Fender} from '../../alexlibs/mui-extension'
import {EntityIcon} from '../../core/EntityIcon'
import {ScButton} from '../../shared/Button/Button'
import {
  mapArrayFromQuerystring,
  mapDateFromQueryString,
  mapDatesToQueryString,
  useQueryString,
} from '../../core/helper/useQueryString'

import {useToast} from '../../core/toast'
import {config} from '../../conf/config'
import {ExportReportsPopper} from '../../shared/ExportPopper/ExportPopperBtn'
import {PeriodPicker} from '../../shared/PeriodPicker/PeriodPicker'
import {useCompaniesContext} from '../../core/context/CompaniesContext'
import {SelectCompaniesByPro} from '../../shared/SelectCompaniesByPro/SelectCompaniesByPro'
import compose from '../../core/helper/compose'
import {Alert, makeSx} from '../../alexlibs/mui-extension'
import {DebouncedInput} from 'shared/DebouncedInput/DebouncedInput'
import {Enum} from '../../alexlibs/ts-utils'
import {cleanObject, openInNew} from '../../core/helper'
import {Report, ReportSearchResult, ReportStatus, ReportStatusPro} from '../../core/client/report/Report'
import {ReportSearch} from '../../core/client/report/ReportSearch'
import {ScOption} from 'core/helper/ScOption'

const css = makeSx({
  card: {
    fontSize: t => styleUtils(t).fontSize.normal,
    display: 'flex',
    alignItems: 'center',
    py: 1,
    px: 2,
  },
  card_content: {
    flex: 1,
  },
  iconDash: {
    my: 0,
    mx: 1,
  },
  card_head: {
    display: 'flex',
    alignItems: 'center',
    mb: 1 / 2,
  },
  filters: {
    mb: 3,
  },
  filtersBody: {
    pb: t => `${t.spacing(1)} !important`,
  },
  actions: {
    flexWrap: 'wrap',
    whiteSpace: 'nowrap',
    mt: 2,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    '& > *': {
      mb: 1,
      ml: 1,
    },
  },
})

const minRowsBeforeDisplayFilters = 2

interface ReportFiltersQs {
  readonly departments?: string[] | string
  readonly siretSirenList?: string[] | string
  start?: string
  end?: string
  status?: string[]
}

export const ReportsPro = () => {
  const _reports = useReportsContext()
  const _companies = useCompaniesContext()

  const {isMobileWidth} = useLayoutContext()
  const history = useHistory()
  const {toastError} = useToast()
  const {formatDate, m} = useI18n()

  const hasFilters = useMemo(() => {
    const {limit, offset, ...values} = _reports.filters
    return Object.keys(cleanObject(values)).length > 0 || offset > 0
  }, [_reports.filters])

  const isFirstVisit = useMemo(
    () => !hasFilters && _reports.list?.entities.every(_ => _.report.status === ReportStatus.TraitementEnCours),
    [_reports.list],
  )

  const displayFilters = useMemo(
    () => (_reports.list && _reports.list.totalCount > minRowsBeforeDisplayFilters) || hasFilters,
    [_reports.list],
  )

  const queryString = useQueryString<Partial<ReportSearch>, Partial<ReportFiltersQs>>({
    toQueryString: mapDatesToQueryString,
    fromQueryString: compose(mapDateFromQueryString, mapArrayFromQuerystring(['status', 'siretSirenList', 'departments'])),
  })

  const filtersCount = useMemo(() => {
    const {offset, limit, ...filters} = _reports.filters
    return Object.keys(cleanObject(filters)).length
  }, [_reports.filters])

  useEffect(() => {
    _companies.accessibleByPro.fetch({force: false})
    _reports.updateFilters({..._reports.initialFilters, ...queryString.get()})
  }, [])

  useEffect(() => {
    ScOption.from(_companies.accessibleByPro.error).map(toastError)
    ScOption.from(_reports.error).map(toastError)
  }, [_reports.error, _companies.accessibleByPro.error])

  useEffect(() => {
    queryString.update(cleanObject(_reports.filters))
  }, [_reports.filters])

  return (
    <Page size="s" loading={_companies.accessibleByPro.loading || _companies.accessibleByPro.loading}>
      <PageTitle
        action={
          <Btn
            variant="outlined"
            color="primary"
            icon="help"
            {...({target: '_blank'} as any)}
            href={config.appBaseUrl + '/comment-ca-marche'}
          >
            {m.help}
            <Icon sx={{ml: 1, color: t => t.palette.text.disabled}}>open_in_new</Icon>
          </Btn>
        }
      >
        {m.reports_pageTitle}
      </PageTitle>

      {isFirstVisit && (
        <Alert type="success" deletable persistentDelete sx={{mb: 2}}>
          <span dangerouslySetInnerHTML={{__html: m.yourAccountIsActivated}} />
        </Alert>
      )}
      {ScOption.from(_companies.accessibleByPro.entity)
        .map(companies => (
          <>
            {displayFilters && (
              <Panel elevation={3} sx={css.filters}>
                <PanelBody sx={css.filtersBody}>
                  <Grid container spacing={1}>
                    <Grid item sm={4} xs={12}>
                      <DebouncedInput
                        value={_reports.filters.siretSirenList}
                        onChange={_ => _reports.updateFilters(prev => ({...prev, siretSirenList: _}))}
                      >
                        {(value, onChange) => (
                          <SelectCompaniesByPro
                            values={value}
                            onChange={onChange}
                            fullWidth
                            sx={{mr: 1}}
                            accessibleCompanies={companies}
                          />
                        )}
                      </DebouncedInput>
                    </Grid>
                    <Grid item sm={4} xs={12}>
                      <DebouncedInput
                        value={_reports.filters.departments}
                        onChange={departments => _reports.updateFilters(prev => ({...prev, departments}))}
                      >
                        {(value, onChange) => (
                          <SelectDepartments label={m.departments} value={value} onChange={onChange} sx={{mr: 1}} fullWidth />
                        )}
                      </DebouncedInput>
                    </Grid>
                    <Grid item sm={4} xs={12}>
                      <DebouncedInput
                        value={_reports.filters.status?.[0] ? Report.getStatusProByStatus(_reports.filters.status[0]) : ''}
                        onChange={e => {
                          const status = ScOption.from(e)
                            .filter(_ => _ in ReportStatusPro)
                            .map(_ => Report.getStatusByStatusPro(_ as ReportStatusPro))
                            .toUndefined()
                          _reports.updateFilters(prev => ({...prev, status}))
                        }}
                      >
                        {(value, onChange) => (
                          <ScSelect
                            value={value}
                            onChange={x => onChange(x.target.value)}
                            id="select-status-pro"
                            label={m.status}
                            fullWidth
                          >
                            <MenuItem value="">&nbsp;</MenuItem>
                            {Enum.values(ReportStatusPro).map(statusPro => (
                              <MenuItem key={statusPro} value={statusPro}>
                                <ReportStatusProLabel dense fullWidth inSelectOptions status={statusPro} />
                              </MenuItem>
                            ))}
                          </ScSelect>
                        )}
                      </DebouncedInput>
                    </Grid>
                  </Grid>
                  <DebouncedInput<[Date | undefined, Date | undefined]>
                    value={[_reports.filters.start, _reports.filters.end]}
                    onChange={([start, end]) => _reports.updateFilters(prev => ({...prev, start, end}))}
                  >
                    {(value, onChange) => <PeriodPicker fullWidth value={value} onChange={onChange} />}
                  </DebouncedInput>
                  <Box sx={css.actions}>
                    <Badge color="error" badgeContent={filtersCount} hidden={filtersCount === 0}>
                      <ScButton icon="clear" onClick={_reports.clearFilters} variant="outlined" color="primary">
                        {m.removeAllFilters}
                      </ScButton>
                    </Badge>
                    <ExportReportsPopper
                      disabled={ScOption.from(_reports?.list?.totalCount)
                        .map(_ => _ > config.reportsLimitForExport)
                        .getOrElse(false)}
                      tooltipBtnNew={ScOption.from(_reports?.list?.totalCount)
                        .map(_ =>
                          _ > config.reportsLimitForExport ? m.cannotExportMoreReports(config.reportsLimitForExport) : '',
                        )
                        .getOrElse('')}
                    >
                      <Btn variant="outlined" color="primary" icon="get_app">
                        {m.exportInXLS}
                      </Btn>
                    </ExportReportsPopper>
                  </Box>
                </PanelBody>
              </Panel>
            )}

            <Panel>
              <Datatable<ReportSearchResult>
                id="reportspro"
                paginate={{
                  minRowsBeforeDisplay: minRowsBeforeDisplayFilters,
                  offset: _reports.filters.offset,
                  limit: _reports.filters.limit,
                  onPaginationChange: pagination => _reports.updateFilters(prev => ({...prev, ...pagination})),
                }}
                data={_reports.list?.entities}
                loading={_reports.fetching}
                total={_reports.list?.totalCount}
                onClickRows={(_, e) => {
                  if (e.metaKey || e.ctrlKey) {
                    openInNew(siteMap.logged.report(_.report.id))
                  } else {
                    history.push(siteMap.logged.report(_.report.id))
                  }
                }}
                columns={
                  isMobileWidth
                    ? [
                        {
                          id: 'all',
                          head: '',
                          render: _ => (
                            <Box sx={css.card}>
                              <Box sx={css.card_content}>
                                <Box sx={css.card_head}>
                                  <Txt bold size="big">
                                    {_.report.companySiret}
                                  </Txt>
                                  <Icon sx={combineSx(css.iconDash, sxUtils.inlineIcon)}>remove</Icon>
                                  <Txt color="disabled">
                                    <Icon sx={sxUtils.inlineIcon}>location_on</Icon>
                                    {_.report.companyAddress.postalCode}
                                  </Txt>
                                </Box>
                                <Txt block color="hint">
                                  {m.thisDate(formatDate(_.report.creationDate))}
                                </Txt>
                                <Txt block color="hint">
                                  {_.report.contactAgreement
                                    ? m.byHim(_.report.firstName + ' ' + _.report.lastName)
                                    : m.anonymousReport}
                                </Txt>
                              </Box>
                              <ReportStatusLabel dense status={_.report.status} />
                            </Box>
                          ),
                        },
                      ]
                    : [
                        {
                          id: 'companyPostalCode',
                          head: m.postalCodeShort,
                          sx: _ =>
                            _.report.status === ReportStatus.TraitementEnCours
                              ? {fontWeight: t => t.typography.fontWeightBold}
                              : undefined,
                          render: _ => _.report.companyAddress.postalCode,
                        },
                        {
                          id: 'siret',
                          head: m.siret,
                          sx: _ =>
                            _.report.status === ReportStatus.TraitementEnCours
                              ? {fontWeight: t => t.typography.fontWeightBold}
                              : undefined,
                          render: _ => _.report.companySiret,
                        },
                        {
                          id: 'createDate',
                          head: m.receivedAt,
                          sx: _ =>
                            _.report.status === ReportStatus.TraitementEnCours
                              ? {fontWeight: t => t.typography.fontWeightBold}
                              : undefined,
                          render: _ => formatDate(_.report.creationDate),
                        },
                        {
                          id: 'status',
                          head: m.status,
                          render: _ => <ReportStatusProLabel dense status={Report.getStatusProByStatus(_.report.status)} />,
                        },
                        {
                          id: 'consumer',
                          head: m.consumer,
                          sx: _ =>
                            _.report.status === ReportStatus.TraitementEnCours
                              ? {fontWeight: t => t.typography.fontWeightBold}
                              : undefined,
                          render: _ =>
                            _.report.contactAgreement ? _.report.firstName + ' ' + _.report.lastName : m.anonymousReport,
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
                      ]
                }
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
          </>
        ))
        .toUndefined()}
    </Page>
  )
}
