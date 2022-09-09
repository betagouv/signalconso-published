import React, {ReactElement, useEffect, useMemo, useState} from 'react'
import {useLayoutContext} from '../../core/Layout/LayoutContext'
import {Autocomplete, Box, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Tooltip} from '@mui/material'
import {useI18n} from '../../core/i18n'
import {ScMenuItem} from '../MenuItem/MenuItem'
import {DialogInputRow} from '../../shared/DialogInputRow/DialogInputRow'
import {Controller, useForm} from 'react-hook-form'
import {ScMultiSelect} from '../../shared/Select/MultiSelect'
import {Enum} from '../../alexlibs/ts-utils'
import {Label} from '../../shared/Label/Label'
import {Btn, Txt} from '../../alexlibs/mui-extension'
import {
  DepartmentDivision,
  IdentificationStatus,
  InvestigationStatus,
  Practice,
  WebsiteWithCompanySearch,
} from '../../core/client/website/Website'
import {ScInput} from '../../shared/Input/ScInput'

export interface WebsitesFiltersProps {
  updateFilters: (_: WebsiteWithCompanySearch) => void
  filters: WebsiteWithCompanySearch
  children: ReactElement<any>
  departmentDivisionList: DepartmentDivision[]
}

interface Form extends WebsiteWithCompanySearch {}

export const WebsitesFilters = ({filters, updateFilters, children, departmentDivisionList, ...props}: WebsitesFiltersProps) => {
  const {m} = useI18n()
  const [open, setOpen] = useState<boolean>(false)
  const close = () => {
    setOpen(false)
  }

  const departmentDivisionMap = useMemo(
    () => new Map(departmentDivisionList.map(obj => [obj.code, obj.name])),
    [departmentDivisionList],
  )

  const confirm = (e: any) => {
    close()
    handleSubmit(updateFilters)(e)
  }

  useEffect(() => {
    reset(filters)
  }, [filters])

  const {
    register,
    handleSubmit,
    control,
    reset,
    getValues,
    watch,
    formState: {errors},
  } = useForm<Form>()

  const layout = useLayoutContext()

  return (
    <>
      {React.cloneElement(children, {
        onClick: (event: any) => {
          setOpen(true)
        },
      })}
      <Dialog maxWidth={'sm'} fullWidth fullScreen={layout.isMobileWidth} open={open ?? false} onClose={close}>
        <DialogTitle>{m.search}</DialogTitle>
        <DialogContent>
          <DialogInputRow icon="check_circle" label={m.practice}>
            <Controller
              defaultValue={filters.practice ?? []}
              name="practice"
              control={control}
              render={({field}) => (
                <ScMultiSelect
                  {...field}
                  fullWidth
                  withSelectAll
                  renderValue={practice => `(${practice.length}) ${practice.join(',')}`}
                >
                  {Enum.values(Practice).map(practice => (
                    <ScMenuItem withCheckbox key={practice} value={practice}>
                      <Label dense {...props}>
                        {practice}
                      </Label>
                    </ScMenuItem>
                  ))}
                </ScMultiSelect>
              )}
            />
          </DialogInputRow>

          <DialogInputRow icon="check_circle" label={m.investigation}>
            <Controller
              defaultValue={filters.investigationStatus ?? []}
              name="investigationStatus"
              control={control}
              render={({field}) => (
                <ScMultiSelect
                  {...field}
                  fullWidth
                  withSelectAll
                  renderValue={investigationStatus =>
                    `(${investigationStatus.length}) ${investigationStatus
                      .map(status => m.InvestigationStatusDesc[status])
                      .join(',')}`
                  }
                >
                  {Enum.values(InvestigationStatus).map(investigationStatus => (
                    <ScMenuItem withCheckbox key={investigationStatus} value={investigationStatus}>
                      <Label dense {...props}>
                        {m.InvestigationStatusDesc[investigationStatus]}
                      </Label>
                    </ScMenuItem>
                  ))}
                </ScMultiSelect>
              )}
            />
          </DialogInputRow>

          <DialogInputRow icon="check_circle" label={m.affectation}>
            <Controller
              defaultValue={filters.attribution ?? []}
              name="attribution"
              control={control}
              render={({field}) => (
                <Autocomplete
                  fullWidth
                  size={'small'}
                  {...props}
                  {...field}
                  onChange={(a, b) => field.onChange(b)}
                  multiple
                  options={departmentDivisionList.map(_ => _.code)}
                  getOptionLabel={option => option}
                  renderInput={params => <ScInput {...params} label={m.affectation} />}
                  renderOption={(props, option) => (
                    <Tooltip title={departmentDivisionMap.get(option) ?? ''} key={props.id}>
                      <li {...props}>
                        <Txt bold>{option}</Txt>
                        <Txt truncate color="hint">
                          &nbsp;-&nbsp;{departmentDivisionMap.get(option)}
                        </Txt>
                      </li>
                    </Tooltip>
                  )}
                  renderTags={(value, getTagProps) =>
                    value.map((option, index) => (
                      <Chip size="small" variant="outlined" label={option} {...getTagProps({index})} />
                    ))
                  }
                />
              )}
            />
          </DialogInputRow>

          <DialogInputRow icon="check_circle" label={m.kind}>
            <Controller
              defaultValue={filters.identificationStatus ?? []}
              name="identificationStatus"
              control={control}
              render={({field}) => (
                <ScMultiSelect
                  {...field}
                  fullWidth
                  withSelectAll
                  renderValue={identificationStatus =>
                    `(${identificationStatus.length}) ${identificationStatus
                      .map(status => m.IdentificationStatusDesc[status])
                      .join(',')}`
                  }
                >
                  {Enum.values(IdentificationStatus).map(kind => (
                    <ScMenuItem withCheckbox key={kind} value={kind}>
                      <Label dense {...props}>
                        {m.IdentificationStatusDesc[kind]}
                      </Label>
                    </ScMenuItem>
                  ))}
                </ScMultiSelect>
              )}
            />
          </DialogInputRow>
        </DialogContent>
        <DialogActions>
          <Btn onClick={close} color="primary">
            {m.close}
          </Btn>
          <Btn onClick={confirm} color="primary" variant="contained">
            {m.search}
          </Btn>
        </DialogActions>
      </Dialog>
    </>
  )
}
