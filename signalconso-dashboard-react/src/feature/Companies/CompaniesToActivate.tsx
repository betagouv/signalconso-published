import {useI18n} from '../../core/i18n'
import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import React, {SyntheticEvent, useEffect} from 'react'
import {useCompaniesContext} from '../../core/context/CompaniesContext'
import {Box, Checkbox, Icon, Tooltip} from '@mui/material'
import {styleUtils, sxUtils} from '../../core/theme'
import {Fender, IconBtn} from '../../alexlibs/mui-extension'
import {Link} from 'react-router-dom'
import {siteMap} from '../../core/siteMap'
import {usePersistentState} from '../../alexlibs/react-persistent-state'
import {useSetState} from '../../alexlibs/react-hooks-lib'
import {ScButton} from '../../shared/Button/Button'
import {useToast} from '../../core/toast'
import {EntityIcon} from '../../core/EntityIcon'
import {AddressComponent} from '../../shared/Address/Address'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {DatatableToolbar} from '../../shared/Datatable/DatatableToolbar'
import {Txt} from '../../alexlibs/mui-extension'
import {Id} from '../../core/model'

export const CompaniesToActivate = () => {
  const {m, formatDate} = useI18n()
  const _companies = useCompaniesContext()
  const _companiesToActivate = _companies.toActivate

  const [selectedCompanies, setSelectedCompanies] = usePersistentState<string[]>([], 'CompaniesToActivate')
  const selectedCompaniesSet = useSetState(selectedCompanies)
  const {toastError, toastErrorIfDefined} = useToast()

  useEffect(() => {
    _companiesToActivate.fetch()
  }, [])

  useEffect(() => {
    toastErrorIfDefined(_companiesToActivate.error)
  }, [_companiesToActivate.error])

  const toggleSelectedCompany = (companyId: Id) => {
    selectedCompaniesSet.has(companyId) ? selectedCompaniesSet.delete(companyId) : selectedCompaniesSet.add(companyId)
    setSelectedCompanies(selectedCompaniesSet.toArray())
  }

  const allChecked = selectedCompaniesSet.size === (_companiesToActivate.list?.entities.length ?? 0)

  const selectAll = () => {
    if (selectedCompaniesSet.size === 0 && !allChecked)
      selectedCompaniesSet.reset(_companiesToActivate.list?.entities.map(_ => _.company.id))
    else selectedCompaniesSet.clear()
    setSelectedCompanies(selectedCompaniesSet.toArray())
  }

  const unselectAll = () => {
    selectedCompaniesSet.clear()
    setSelectedCompanies(selectedCompaniesSet.toArray())
  }

  const confirmCompaniesPosted = (event: SyntheticEvent<any>, closeDialog: () => void) => {
    _companies.confirmCompaniesPosted
      .fetch({}, selectedCompaniesSet.toArray())
      .then(() => {
        _companiesToActivate.fetch({clean: false})
      })
      .then(unselectAll)
      .catch(toastError)
      .finally(closeDialog)
  }

  return (
    <Panel sx={{overflow: 'visible'}}>
      <Datatable
        id="companiestoactivate"
        header={
          <DatatableToolbar
            onClear={selectedCompaniesSet.clear}
            open={!_companiesToActivate.fetching && selectedCompaniesSet.size > 0}
            actions={
              <>
                <ScButton
                  disabled={_companiesToActivate.fetching || selectedCompaniesSet.size === 0}
                  loading={_companies.downloadActivationDocument.loading}
                  color="primary"
                  variant="outlined"
                  icon="file_download"
                  sx={{mr: 1}}
                  onClick={() =>
                    _companies.downloadActivationDocument.fetch({}, selectedCompaniesSet.toArray()).catch(toastError)
                  }
                >
                  {m.download}
                </ScButton>
                <ScDialog title={m.validateLetterSentTitle} content={m.validateLetterSentDesc} onConfirm={confirmCompaniesPosted}>
                  <ScButton
                    disabled={_companiesToActivate.fetching || selectedCompaniesSet.size === 0}
                    loading={_companies.confirmCompaniesPosted.loading}
                    sx={{mr: 1}}
                    color="error"
                    variant="contained"
                    icon="check_circle"
                  >
                    {m.validateLetterSent}
                  </ScButton>
                </ScDialog>
              </>
            }
          >
            <Txt bold>{selectedCompaniesSet.size}</Txt>&nbsp;{m.selectedCompanies}
          </DatatableToolbar>
        }
        loading={_companiesToActivate.fetching}
        data={_companiesToActivate.list?.entities}
        paginate={{
          offset: _companiesToActivate.filters.offset,
          limit: _companiesToActivate.filters.limit,
          onPaginationChange: pagination => _companiesToActivate.updateFilters(prev => ({...prev, ...pagination})),
        }}
        total={_companiesToActivate.list?.totalCount}
        getRenderRowKey={_ => _.company.id}
        showColumnsToggle={true}
        rowsPerPageOptions={[5, 10, 25, 100, 250]}
        columns={[
          {
            head: (
              <Checkbox
                indeterminate={!allChecked && selectedCompaniesSet.size > 0}
                checked={allChecked}
                disabled={_companiesToActivate.fetching}
                onClick={selectAll}
              />
            ),
            alwaysVisible: true,
            id: 'select',
            render: _ => (
              <Checkbox checked={selectedCompaniesSet.has(_.company.id)} onClick={() => toggleSelectedCompany(_.company.id)} />
            ),
          },
          {
            id: 'siret',
            head: m.name,
            sx: _ => ({
              lineHeight: 1.4,
              maxWidth: 390,
            }),
            render: _ => (
              <Tooltip title={_.company.name}>
                <span>
                  <Box component="span" sx={{marginBottom: '-1px', fontWeight: t => t.typography.fontWeightBold}}>
                    {_.company.name}
                  </Box>
                  <br />
                  <Box component="span" sx={{fontSize: t => styleUtils(t).fontSize.small, color: t => t.palette.text.disabled}}>
                    {_.company.siret}
                  </Box>
                </span>
              </Tooltip>
            ),
          },
          {
            head: m.address,
            id: 'address',
            sx: _ => ({
              pt: 0.5,
              pb: 0.5,
              fontSize: t => styleUtils(t).fontSize.small,
              color: t => t.palette.text.secondary,
              maxWidth: 300,
              ...sxUtils.truncate,
            }),
            render: _ => (
              <Tooltip title={<AddressComponent address={_.company.address} />}>
                <span>
                  <AddressComponent address={_.company.address} />
                </span>
              </Tooltip>
            ),
          },
          {
            head: m.created_at,
            id: 'tokenCreation',
            render: _ => <>{formatDate(_.tokenCreation)}</>,
          },
          {
            head: m.lastNotice,
            id: 'lastNotice',
            render: _ => <>{formatDate(_.lastNotice)}</>,
          },
          {
            id: 'actions',
            sx: _ => sxUtils.tdActions,
            stickyEnd: true,
            render: _ => (
              <>
                <Link target="_blank" to={siteMap.logged.reports({siretSirenList: [_.company.siret]})}>
                  <Tooltip title={m.reports}>
                    <IconBtn color="primary">
                      <Icon>chevron_right</Icon>
                    </IconBtn>
                  </Tooltip>
                </Link>
              </>
            ),
          },
        ]}
        renderEmptyState={<Fender title={m.noCompanyFound} icon={EntityIcon.company} />}
      />
    </Panel>
  )
}
