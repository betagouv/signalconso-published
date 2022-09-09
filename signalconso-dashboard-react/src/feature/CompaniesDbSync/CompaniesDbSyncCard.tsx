import {Txt} from '../../alexlibs/mui-extension'
import {Panel, PanelBody} from 'shared/Panel'
import {alpha, Box, CircularProgress, Grid, useTheme} from '@mui/material'
import {styleUtils} from '../../core/theme'
import {useI18n} from '../../core/i18n'
import {PanelFoot} from '../../shared/Panel/PanelFoot'
import {ScButton} from '../../shared/Button/Button'
import {UseAsync, useMemoFn} from '../../alexlibs/react-hooks-lib'
import {CompaniesDbSyncInfo} from '../../core/client/companies-db-sync/CompaniesDbSync'

interface Props {
  name: string
  info?: CompaniesDbSyncInfo
  start: UseAsync<() => Promise<void>>
  cancel: UseAsync<() => Promise<void>>
}

const progressSize = 110

export const CompaniesDbSyncCard = ({name, info, start, cancel}: Props) => {
  const t = useTheme()
  const {m, dateFromNow, formatLargeNumber} = useI18n()
  const percent = useMemoFn(info, _ => (_.linesDone / _.linesCount) * 100)

  return (
    <Panel>
      <PanelBody>
        <Box
          sx={{
            mb: 1,
            height: progressSize,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
          }}
        >
          <CircularProgress
            value={100}
            size={progressSize}
            variant={info && !info.endedAt ? 'indeterminate' : 'determinate'}
            sx={{
              color: t => alpha(t.palette.primary.light, 0.2),
              height: progressSize,
              width: progressSize,
              position: 'absolute',
            }}
          />
          <CircularProgress value={percent} size={progressSize} variant="determinate" />
          <Box
            sx={{
              position: 'absolute',
              fontSize: t => styleUtils(t).fontSize.big,
              ...(!info?.endedAt && {
                fontWeight: t.typography.fontWeightBold,
                color: t.palette.primary.main,
              }),
            }}
          >
            {Math.round(percent ?? 0)} %
          </Box>
        </Box>
        <div style={{textAlign: 'center'}}>
          <Txt size="big">{formatLargeNumber(info?.linesDone ?? 0)}</Txt>
          <Txt color="hint">
            &nbsp;/&nbsp;
            {formatLargeNumber(info?.linesCount ?? 0)}
          </Txt>
        </div>

        <Txt block bold size="big" sx={{mt: 3}}>
          {info?.fileName ?? name}
        </Txt>
        <a href={info?.fileUrl ?? '#'}>
          <Txt block truncate gutterBottom link>
            {info?.fileUrl ?? ''}
          </Txt>
        </a>

        {info && (
          <Grid container sx={{mt: 2}}>
            <Grid item xs={6}>
              <Txt block size="small" color="hint" uppercase>
                {m.beginning}
              </Txt>
              <Txt block size="big">
                {dateFromNow(info.startedAt)}
              </Txt>
            </Grid>
            <Grid item xs={6} style={{textAlign: 'right'}}>
              {info.endedAt ? (
                <>
                  <Txt block size="small" color="hint" uppercase>
                    {m.end}
                  </Txt>
                  <Txt block size="big">
                    {dateFromNow(info.endedAt)}
                  </Txt>
                </>
              ) : (
                <Txt block size="title" bold sx={{color: t => t.palette.warning.main}}>
                  {m.inProgress}
                </Txt>
              )}
            </Grid>
          </Grid>
        )}
      </PanelBody>
      <PanelFoot border>
        {(!info || info.endedAt) && (
          <ScButton color="primary" icon="play_arrow" onClick={start.call} loading={start.loading}>
            {m.startUp}
          </ScButton>
        )}
        {info && !info.endedAt && (
          <ScButton sx={{color: t => t.palette.error.main}} icon="stop" onClick={cancel.call} loading={cancel.loading}>
            {m.cancel}
          </ScButton>
        )}
      </PanelFoot>
    </Panel>
  )
}
