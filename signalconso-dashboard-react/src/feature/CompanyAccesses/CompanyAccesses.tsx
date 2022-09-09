import React, {useEffect, useMemo} from 'react'
import {Page, PageTitle} from '../../shared/Layout'
import {useI18n} from '../../core/i18n'
import {useParams} from 'react-router'
import {Datatable} from '../../shared/Datatable/Datatable'
import {Icon, Tooltip} from '@mui/material'
import {Panel} from '../../shared/Panel'
import {Txt} from '../../alexlibs/mui-extension'
import {IconBtn} from '../../alexlibs/mui-extension'
import {useLogin} from '../../core/context/LoginContext'
import {useCompanyAccess} from './useCompaniesAccess'
import {CompanyAccessCreateBtn} from './CompanyAccessCreateBtn'
import {useToast} from '../../core/toast'
import {SaveUndeliveredDocBtn} from './SaveUndeliveredDocBtn'
import {useCompaniesContext} from '../../core/context/CompaniesContext'
import {Enum} from '../../alexlibs/ts-utils'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {ScButton} from '../../shared/Button/Button'
import {ScRadioGroupItem} from '../../shared/RadioGroup/RadioGroupItem'
import {ScRadioGroup} from '../../shared/RadioGroup/RadioGroup'
import {siteMap} from '../../core/siteMap'
import {sxUtils} from '../../core/theme'
import {getAbsoluteLocalUrl, toQueryString} from '../../core/helper'
import {CompanyAccessLevel} from '../../core/client/company-access/CompanyAccess'
import {Id} from '../../core/model'
import {ScOption} from 'core/helper/ScOption'

interface Accesses {
  name?: string
  userId?: Id
  email?: string
  level: CompanyAccessLevel
  tokenId?: Id
  token?: string
  editable?: Boolean
  isHeadOffice?: Boolean
}

export const CompanyAccesses = () => {
  const {siret} = useParams<{siret: string}>()

  const _crudAccess = useCompanyAccess(useLogin().apiSdk, siret).crudAccess
  const _crudToken = useCompanyAccess(useLogin().apiSdk, siret).crudToken
  const _company = useCompaniesContext()

  const {m} = useI18n()
  const {connectedUser} = useLogin()
  const {toastSuccess, toastError, toastErrorIfDefined} = useToast()

  const copyActivationLink = (token: string) => {
    const patch = getAbsoluteLocalUrl(siteMap.loggedout.activatePro(siret) + toQueryString({token}))
    const activationLink = window.location.host + patch
    navigator.clipboard.writeText(activationLink).then(_ => toastSuccess(m.addressCopied))
  }

  const accesses: Accesses[] = useMemo(() => {
    return [
      ...(_crudAccess.list ?? []).map(_ => ({
        email: _.email,
        name: _.firstName + ' ' + _.lastName,
        level: _.level,
        userId: _.userId,
        editable: _.editable,
        isHeadOffice: _.isHeadOffice,
      })),
      ...(_crudToken.list ?? []).map(_ => ({email: _.emailedTo, level: _.level, tokenId: _.id, token: _.token})),
    ]
  }, [_crudAccess.list, _crudToken.list])

  useEffect(() => {
    _crudAccess.fetch()
    _crudToken.fetch()
  }, [])

  useEffect(() => {
    toastErrorIfDefined(_crudToken.fetchError)
  }, [_crudToken.list, _crudToken.fetchError])

  useEffect(() => {
    toastErrorIfDefined(_crudAccess.fetchError)
  }, [_crudAccess.list, _crudAccess.fetchError])

  const inviteNewUser = async (email: string, level: CompanyAccessLevel): Promise<any> => {
    if (accesses?.find(_ => _.email === email)) {
      toastError({message: m.invitationToProAlreadySent(email)})
    } else {
      await _crudToken
        .create({}, email, level)
        .then(() => toastSuccess(m.userInvitationSent))
        .then(() => window.location.reload())
    }
  }

  return (
    <Page size="m">
      <PageTitle
        action={
          !connectedUser.isDGCCRF && (
            <>
              {_crudAccess.list?.length === 0 && (
                <SaveUndeliveredDocBtn
                  loading={_company.saveUndeliveredDocument.loading}
                  onChange={async date => {
                    if (date) return _company.saveUndeliveredDocument.fetch({}, siret, date)
                    else throw new Error("Can't save with an empty date")
                  }}
                  sx={{mr: 1}}
                />
              )}
              <CompanyAccessCreateBtn
                loading={_crudToken.creating}
                onCreate={inviteNewUser}
                errorMessage={_crudToken.createError}
              />
            </>
          )
        }
      >
        {m.companyAccessesTitle}
      </PageTitle>
      <Panel>
        <Datatable
          id="companyaccesses"
          data={_crudAccess.list && _crudToken.list ? accesses : undefined}
          loading={_crudAccess.fetching || _crudToken.fetching}
          getRenderRowKey={_ => _.email ?? _.tokenId!}
          columns={[
            {
              id: 'status',
              head: '',
              render: _ =>
                _.name ? (
                  <Icon sx={{color: t => t.palette.success.light}}>check_circle</Icon>
                ) : (
                  <Icon sx={{color: t => t.palette.warning.main}}>watch_later</Icon>
                ),
            },
            {
              id: 'email',
              head: m.email,
              render: _ => (
                <>
                  <div>
                    <Txt bold>{_.email}</Txt>
                    &nbsp;
                    {_.isHeadOffice && <Txt color="hint">({m.isHeadOffice})</Txt>}
                    {connectedUser.email === _.email && <Txt color="hint">({m.you})</Txt>}
                  </div>
                  <Txt size="small" color="hint">
                    {_.name}
                  </Txt>
                </>
              ),
            },
            {
              id: 'level',
              head: m.companyAccessLevel,
              render: _ => (
                <ScDialog
                  maxWidth="xs"
                  title={m.editAccess}
                  content={close => (
                    <ScRadioGroup
                      value={_.level}
                      onChange={level => {
                        if (_.userId) _crudAccess.update(_.userId, level as CompanyAccessLevel)
                        close()
                      }}
                    >
                      {Enum.keys(CompanyAccessLevel).map(level => (
                        <ScRadioGroupItem
                          title={CompanyAccessLevel[level]}
                          description={m.companyAccessLevelDescription[CompanyAccessLevel[level]]}
                          value={level}
                          key={level}
                        />
                      ))}
                    </ScRadioGroup>
                  )}
                >
                  <Tooltip title={m.editAccess}>
                    <ScButton
                      sx={{textTransform: 'capitalize'}}
                      loading={_crudAccess.updating(_.userId ?? '')}
                      color="primary"
                      icon="manage_accounts"
                      variant="outlined"
                      disabled={!_.userId || !_.editable}
                    >
                      {(CompanyAccessLevel as any)[_.level]}
                    </ScButton>
                  </Tooltip>
                </ScDialog>
              ),
            },
            {
              id: 'action',
              head: '',
              sx: _ => sxUtils.tdActions,
              render: _ => (
                <>
                  {connectedUser.isAdmin &&
                    !_.name &&
                    ScOption.from(_.email)
                      .map(email => (
                        <ScDialog
                          title={m.resendCompanyAccessToken(_.email)}
                          onConfirm={(event, close) =>
                            _crudToken
                              .create({preventInsert: true}, email, _.level)
                              .then(_ => close())
                              .then(_ => toastSuccess(m.userInvitationSent))
                          }
                          maxWidth="xs"
                        >
                          <Tooltip title={m.resendInvite}>
                            <IconBtn>
                              <Icon>send</Icon>
                            </IconBtn>
                          </Tooltip>
                        </ScDialog>
                      ))
                      .getOrElse(<></>)}
                  {connectedUser.isAdmin &&
                    !_.name &&
                    ScOption.from(_.token)
                      .map(token => (
                        <Tooltip title={m.copyInviteLink}>
                          <IconBtn onClick={_ => copyActivationLink(token)}>
                            <Icon>content_copy</Icon>
                          </IconBtn>
                        </Tooltip>
                      ))
                      .getOrElse(<></>)}
                  {ScOption.from(_)
                    .filter(_ => _.editable === true)
                    .flatMap(_ => _.userId)
                    .map(userId => (
                      <ScDialog
                        title={m.deleteCompanyAccess(_.name!)}
                        onConfirm={() => _crudAccess.remove(userId)}
                        maxWidth="xs"
                        confirmLabel={m.delete}
                      >
                        <Tooltip title={m.delete}>
                          <IconBtn loading={_crudAccess.removing(userId)}>
                            <Icon>delete</Icon>
                          </IconBtn>
                        </Tooltip>
                      </ScDialog>
                    ))
                    .getOrElse(
                      ScOption.from(_.tokenId)
                        .map(tokenId => (
                          <ScDialog
                            title={m.deleteCompanyAccessToken(_.email)}
                            onConfirm={() => _crudToken.remove(tokenId)}
                            maxWidth="xs"
                          >
                            <IconBtn loading={_crudToken.removing(tokenId)}>
                              <Icon>delete</Icon>
                            </IconBtn>
                          </ScDialog>
                        ))
                        .getOrElse(<></>),
                    )}
                </>
              ),
            },
          ]}
        />
      </Panel>
    </Page>
  )
}
