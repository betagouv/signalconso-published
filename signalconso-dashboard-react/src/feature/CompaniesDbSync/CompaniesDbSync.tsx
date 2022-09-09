import React, {useEffect} from 'react'
import {Page, PageTitle} from 'shared/Layout'
import {useCompaniesDbSyncContext} from '../../core/context/CompaniesDbSyncContext'
import {useI18n} from '../../core/i18n'
import {CompaniesDbSyncCard} from './CompaniesDbSyncCard'
import {Grid} from '@mui/material'
import {ScButton} from '../../shared/Button/Button'
import {Alert} from '../../alexlibs/mui-extension'

export const CompaniesDbSync = () => {
  const {m} = useI18n()
  const _companiesDbSync = useCompaniesDbSyncContext()

  useEffect(() => {
    _companiesDbSync.getInfo.fetch()
    const timer = setInterval(() => {
      _companiesDbSync.getInfo.fetch({clean: false})
    }, 4000)
    return () => clearInterval(timer)
  }, [])

  return (
    <Page>
      <PageTitle
        action={
          <ScButton
            color="error"
            variant="contained"
            icon="clear"
            loading={_companiesDbSync.cancelAllFiles.loading}
            onClick={_companiesDbSync.cancelAllFiles.call}
          >
            {m.cancelAll}
          </ScButton>
        }
      >
        {m.database}
      </PageTitle>
      <Alert type="info" sx={{mb: 2}}>
        <div dangerouslySetInnerHTML={{__html: m.companiesDbSyncInfo}} />
      </Alert>
      {_companiesDbSync.getInfo.entity && (
        <Grid container spacing={2}>
          <Grid item sm={6}>
            <CompaniesDbSyncCard
              name="StockEtablissement"
              start={_companiesDbSync.startEtablissementFile}
              cancel={_companiesDbSync.cancelEtablissementFile}
              info={_companiesDbSync.getInfo.entity.etablissementImportInfo}
            />
          </Grid>
          <Grid item sm={6}>
            <CompaniesDbSyncCard
              name="StockUniteLegale"
              start={_companiesDbSync.startUniteLegaleFile}
              cancel={_companiesDbSync.cancelUniteLegaleFile}
              info={_companiesDbSync.getInfo.entity.uniteLegaleInfo}
            />
          </Grid>
        </Grid>
      )}
    </Page>
  )
}
