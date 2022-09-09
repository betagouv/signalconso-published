import {Page, PageTitle} from 'shared/Layout'
import {useI18n} from '../../core/i18n'
import {Panel, PanelBody, PanelHead} from '../../shared/Panel'
import {useFetcher} from '../../alexlibs/react-hooks-lib'
import {useLogin} from '../../core/context/LoginContext'
import {useAsync, useEffectFn} from '../../alexlibs/react-hooks-lib'
import {useEffect} from 'react'
import {Box, Icon} from '@mui/material'
import {Txt} from '../../alexlibs/mui-extension'
import {Alert, IconBtn} from '../../alexlibs/mui-extension'
import {useToast} from '../../core/toast'
import {capitalize} from '../../core/helper'

export const Admin = () => {
  const {m} = useI18n()
  const {apiSdk: api, connectedUser} = useLogin()
  const {toastError} = useToast()

  const _emailCodes = useFetcher<() => Promise<{dgccrf: string[]; pro: string[]; consumer: string[]}>>(() =>
    api.secured.admin
      .getEmailCodes()
      .then(emailCodes => emailCodes.sort())
      .then(emailCodes => ({
        dgccrf: emailCodes.filter(_ => _.startsWith('dgccrf.')),
        pro: emailCodes.filter(_ => _.startsWith('pro.')),
        consumer: emailCodes.filter(_ => _.startsWith('consumer.')),
      })),
  )
  const _sendEmail = useAsync(api.secured.admin.sendTestEmail)

  useEffect(() => {
    _emailCodes.fetch()
  }, [])

  useEffectFn(_emailCodes.error, toastError)
  useEffectFn(_sendEmail.error, toastError)

  return (
    <Page size="s">
      <PageTitle>{m.menu_admin}</PageTitle>
      <Panel loading={_emailCodes.loading}>
        <PanelHead>{m.sendDummyEmail}</PanelHead>
        <PanelBody>
          <Alert type="info" gutterBottom>
            <div dangerouslySetInnerHTML={{__html: m.allMailsWillBeSendTo(connectedUser.email)}} />
          </Alert>

          {_emailCodes.entity &&
            Object.entries(_emailCodes.entity).map(([type, emailCodes]) => (
              <Box key={type} sx={{mt: 3, mb: 4}}>
                <Txt size="big" bold>
                  {capitalize(type)}
                </Txt>
                {emailCodes.map(emailCode => (
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      py: 1.5,
                      borderBottom: t => `1px solid ${t.palette.divider}`,
                    }}
                  >
                    <Box
                      sx={{
                        flex: 1,
                      }}
                    >
                      {(() => {
                        try {
                          const {title, desc} = (m.testMails as any)[type][emailCode.split('.')[1]]
                          return (
                            <>
                              <Txt bold block>
                                {title}
                              </Txt>
                              <Txt color="hint" block dangerouslySetInnerHTML={{__html: desc}} />
                              <Txt color="disabled" size="small" block>
                                {emailCode}
                              </Txt>
                            </>
                          )
                        } catch (e) {
                          console.error(`Missing translation for ${emailCode}`)
                          return <></>
                        }
                      })()}
                    </Box>
                    <Box>
                      <IconBtn
                        color="primary"
                        loading={_sendEmail.loading}
                        onClick={() => _sendEmail.call(emailCode, connectedUser.email)}
                      >
                        <Icon>send</Icon>
                      </IconBtn>
                    </Box>
                  </Box>
                ))}
              </Box>
            ))}
        </PanelBody>
      </Panel>
    </Page>
  )
}
