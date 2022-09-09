import {Panel, PanelBody} from '../../shared/Panel'
import {ReportStatusLabel} from '../../shared/ReportStatus/ReportStatus'
import {Alert} from '../../alexlibs/mui-extension'
import {ReportCategories} from './ReportCategories'
import {Box, Icon} from '@mui/material'
import {PanelFoot} from '../../shared/Panel/PanelFoot'
import {ScChip} from '../../shared/Chip/ScChip'
import React, {ReactNode} from 'react'
import {styleUtils} from '../../core/theme'
import {useI18n} from '../../core/i18n'
import {makeSx} from '../../alexlibs/mui-extension'
import {Report} from '../../core/client/report/Report'

const css = makeSx({
  root: {
    transition: t => t.transitions.create('box-shadow'),
  },
  pageTitle: {
    display: 'flex',
    alignItems: 'flex-start',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    mb: 2,
  },
  pageTitle_txt: {
    margin: 0,
    fontSize: t => styleUtils(t).fontSize.bigTitle,
  },
  actions: {
    flexWrap: 'wrap',
    whiteSpace: 'nowrap',
  },
})

interface Props {
  report: Report
  elevated?: boolean
  children?: ReactNode
  hideTags?: boolean
  hideSiret?: boolean
}

export const ReportHeader = ({hideTags, hideSiret, report, children, elevated}: Props) => {
  const {m} = useI18n()

  return (
    <Panel elevation={elevated ? 3 : 0} sx={css.root}>
      <PanelBody>
        <Box sx={css.pageTitle}>
          <div>
            <Box component="h1" sx={css.pageTitle_txt}>
              {m.report_pageTitle}
              {!hideSiret && (
                <>
                  &nbsp;
                  {report.companySiret}
                </>
              )}
            </Box>
            {!hideSiret && <Box sx={{color: t => t.palette.text.disabled}}>{report.companyName}</Box>}
            <Box sx={{color: t => t.palette.text.disabled}}>ID {report.id}</Box>
          </div>
          <ReportStatusLabel style={{marginLeft: 'auto'}} status={report.status} />
        </Box>
        <Alert id="report-info" dense type="info" deletable persistentDelete sx={{mb: 2}}>
          {m.reportCategoriesAreSelectByConsumer}
        </Alert>
        <ReportCategories categories={[report.category, ...report.subcategories]} />
      </PanelBody>
      {(!hideTags || children) && (
        <PanelFoot sx={css.actions} border>
          {!hideTags && (
            <div style={{flex: 1}}>
              {report.tags.map(tag => [
                <ScChip
                  icon={
                    <Icon style={{fontSize: 20}} sx={{color: t => t.palette.text.disabled}}>
                      sell
                    </Icon>
                  }
                  key={tag}
                  label={tag}
                />,
                ' ',
              ])}
            </div>
          )}
          {children}
        </PanelFoot>
      )}
    </Panel>
  )
}
