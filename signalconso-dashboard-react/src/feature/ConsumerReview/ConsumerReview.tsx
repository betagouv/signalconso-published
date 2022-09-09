import {Page} from 'shared/Layout'
import {Panel, PanelBody, PanelHead} from '../../shared/Panel'
import {ScRadioGroup} from '../../shared/RadioGroup/RadioGroup'
import {useI18n} from '../../core/i18n'
import {ScRadioGroupItem} from '../../shared/RadioGroup/RadioGroupItem'
import {ScInput} from '../../shared/Input/ScInput'
import {Txt} from '../../alexlibs/mui-extension'
import {ScButton} from '../../shared/Button/Button'
import {PanelFoot} from '../../shared/Panel/PanelFoot'
import {Controller, useForm} from 'react-hook-form'
import {useAsync} from '../../alexlibs/react-hooks-lib'
import React, {useEffect, useState} from 'react'
import {useLocation, useParams} from 'react-router'
import {useToast} from '../../core/toast'
import {Alert} from '../../alexlibs/mui-extension'
import {QueryString} from '../../core/helper/useQueryString'
import {Box} from '@mui/material'
import {useLayoutContext} from '../../core/Layout/LayoutContext'
import {Emoticon} from '../../shared/Emoticon/Emoticon'
import {Id, ResponseConsumerReview, ResponseEvaluation} from '../../core/model'

interface Props {
  onSubmit: (reportId: Id, review: ResponseConsumerReview) => Promise<any>
}

interface Form {
  evaluation: ResponseEvaluation
  details?: string
}

export const ConsumerReview = ({onSubmit}: Props) => {
  const {reportId} = useParams<{reportId: Id}>()
  const {m} = useI18n()
  const {toastError, toastErrorIfDefined} = useToast()
  const [done, setDone] = useState(false)
  const {
    register,
    handleSubmit,
    control,
    formState: {errors, isValid},
  } = useForm<Form>()
  const {search} = useLocation()

  const getEvaluationFromQueryString = (qs: string): ResponseEvaluation | undefined => {
    const parsed = QueryString.parse(qs.replace(/^\?/, '')).evaluation as unknown as ResponseEvaluation
    return ResponseEvaluation[parsed]
  }

  const _post = useAsync(onSubmit)
  const {isMobileWidth} = useLayoutContext()

  const submit = async (form: Form) => {
    await _post.call(reportId, {...form})
    setDone(true)
  }
  useEffect(() => {
    toastErrorIfDefined(_post.error)
  }, [_post.error])

  return (
    <Page size="s">
      {done ? (
        <Alert type="success" sx={{mb: 2}}>
          {m.thanksForSharingYourMind}
        </Alert>
      ) : (
        <form onSubmit={handleSubmit(submit)}>
          <Panel>
            <PanelHead>{m.shareYourMind}</PanelHead>

            <PanelBody>
              <Txt block gutterBottom color="hint" dangerouslySetInnerHTML={{__html: m.didTheCompanyAnsweredWell}} />
              <Controller
                name="evaluation"
                defaultValue={getEvaluationFromQueryString(search)}
                rules={{required: {value: true, message: m.required}}}
                control={control}
                render={({field}) => (
                  <ScRadioGroup sx={{mt: 3}} inline={!isMobileWidth} error={!!errors.evaluation} {...field}>
                    <ScRadioGroupItem value={ResponseEvaluation.Positive}>
                      <Emoticon sx={{fontSize: 50}} aria-label="happy">
                        😀
                      </Emoticon>
                    </ScRadioGroupItem>
                    <ScRadioGroupItem value={ResponseEvaluation.Neutral}>
                      <Emoticon sx={{fontSize: 50}} aria-label="neutral">
                        😐
                      </Emoticon>
                    </ScRadioGroupItem>
                    <ScRadioGroupItem value={ResponseEvaluation.Negative}>
                      <Emoticon sx={{fontSize: 50}} aria-label="sad">
                        🙁
                      </Emoticon>
                    </ScRadioGroupItem>
                  </ScRadioGroup>
                )}
              />

              <Txt sx={{mt: 3}} block color="hint" dangerouslySetInnerHTML={{__html: m.youCanAddCommentForDGCCRF}} />
              <ScInput {...register('details')} multiline fullWidth rows={5} maxRows={12} />
            </PanelBody>
            <PanelFoot alignEnd>
              <ScButton loading={_post.loading} type="submit" icon="send" variant="contained" color="primary">
                {m.send}
              </ScButton>
            </PanelFoot>
          </Panel>
        </form>
      )}
      <div>
        <Txt block gutterBottom color="disabled">
          {m.youCanNoteSignalConso}
        </Txt>
        <Box
          component="a"
          sx={{mt: 1}}
          href="https://monavis.numerique.gouv.fr/Demarches/2071?&view-mode=formulaire-avis&nd_mode=en-ligne-enti%C3%A8rement&nd_source=button&key=5a58254dab900906fe4924e37c1c5bba"
        >
          <img
            src="https://monavis.numerique.gouv.fr/monavis-static/bouton-bleu.png"
            alt="Je donne mon avis sur voxusagers.gouv.fr"
            title="Je donne mon avis sur cette démarche"
          />
        </Box>
      </div>
    </Page>
  )
}
