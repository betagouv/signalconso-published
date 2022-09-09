import {Autocomplete, Chip, Skeleton, Tooltip} from '@mui/material'
import {ScInput} from '../Input/ScInput'
import {Txt} from '../../alexlibs/mui-extension'
import {forwardRef, useEffect} from 'react'
import {useFetcher, useMemoFn} from '../../alexlibs/react-hooks-lib'
import {AutocompleteProps} from '@mui/material/Autocomplete'

interface Props
  extends Pick<
    AutocompleteProps<string, true, false, false>,
    'value' | 'defaultValue' | 'className' | 'style' | 'placeholder' | 'onChange' | 'fullWidth'
  > {
  label?: string
}

export const SelectActivityCode = forwardRef((props: Props, ref) => {
  const _activityCodes = useFetcher(() => import('../../core/activityCodes').then(_ => _.activityCodes))
  useEffect(() => {
    _activityCodes.fetch()
  }, [])

  const activityCodes = useMemoFn(_activityCodes.entity, _ => Object.keys(_).sort())
  const activities = _activityCodes.entity

  return (
    <>
      {_activityCodes.loading ? (
        <Skeleton height={50} />
      ) : (
        activityCodes &&
        activities && (
          <Autocomplete
            {...props}
            ref={ref}
            multiple
            options={activityCodes}
            getOptionLabel={option => option + activities[option]}
            renderInput={params => <ScInput {...params} label={props.label} />}
            renderOption={(props, option: string) => (
              <Tooltip title={activities[option]} key={props.id}>
                <li {...props}>
                  <Txt bold>{option}</Txt>
                  <Txt truncate color="hint">
                    &nbsp;-&nbsp;{activities[option]}
                  </Txt>
                </li>
              </Tooltip>
            )}
            renderTags={(value, getTagProps) =>
              value.map((option, index) => <Chip size="small" variant="outlined" label={option} {...getTagProps({index})} />)
            }
          />
        )
      )}
    </>
  )
})
