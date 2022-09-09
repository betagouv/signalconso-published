import {Box, Checkbox, Icon, Menu, MenuItem} from '@mui/material'
import * as React from 'react'
import {useEffect, useMemo} from 'react'
import {useSetState, UseSetState} from '../../alexlibs/react-hooks-lib'
import {useConstantContext} from '../../core/context/ConstantContext'

import {useI18n} from '../../core/i18n'
import {makeSx} from '../../alexlibs/mui-extension'
import {combineSx} from '../../core/theme'
import {Region} from '../../core/client/constant/Country'
import {stopPropagation} from '../../core/helper'
import {ScOption} from 'core/helper/ScOption'

const withRegions =
  (WrappedComponent: React.ComponentType<SelectDepartmentsMenuProps>) => (props: Omit<SelectDepartmentsMenuProps, 'regions'>) => {
    const {regions} = useConstantContext()
    useEffect(() => {
      regions.fetch({force: false})
    }, [])
    return ScOption.from(regions.entity)
      .map(_ => <WrappedComponent {...props} regions={_} />)
      .getOrElse(<></>)
  }

const css = makeSx({
  regionLabel: {
    fontWeight: t => t.typography.fontWeightBold,
    flex: 1,
  },
  regionToggleArrow: {
    width: 36,
    height: 36,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    ml: 1,
    borderRadius: 30,
    color: t => t.palette.text.disabled,
    '&:hover, &:active, &:focus': {
      background: t => t.palette.action.hover,
      color: t => t.palette.primary.main,
    },
  },
  menuItem: {
    py: 0,
    px: 1 / 2,
  },
  menuItemRegion: {
    borderBottom: t => `1px solid ${t.palette.divider}`,
  },
  cbDepartment: {
    paddingTop: `6px !important`,
    paddingBottom: `6px !important`,
  },
})

interface SelectDepartmentsMenuProps {
  onClose: () => void
  regions: Region[]
  initialValues?: string[]
  selectAllLabel?: string
  anchorEl: HTMLElement | null
  open: boolean
  onChange: (departments: string[]) => void
}

export const SelectDepartmentsMenu = withRegions(
  ({selectAllLabel, onClose, regions, initialValues, anchorEl, open, onChange}: SelectDepartmentsMenuProps) => {
    const indexValues: UseSetState<string> = useSetState<string>()
    const openedRegions: UseSetState<string> = useSetState<string>()
    const allDepartments = useMemo(() => regions.flatMap(_ => _.departments).map(_ => _.code), [])
    const allDepartmentsSelected = allDepartments.every(_ => indexValues.has(_))
    const someDepartmentsSelected = !!allDepartments.find(_ => indexValues.has(_))
    const {m} = useI18n()
    selectAllLabel = selectAllLabel || m.selectAllDepartments

    useEffect(() => {
      indexValues.reset(initialValues)
    }, [initialValues])

    const onSelectDepartments = (departments: string[]) => {
      indexValues.toggleAll(departments)
      onChange(indexValues.toArray())
    }

    const onSelectDepartment = (department: string) => {
      indexValues.toggle(department)
      onChange(indexValues.toArray())
    }

    const onSelectAll = () => {
      allDepartments.forEach(allDepartmentsSelected ? indexValues.delete : indexValues.add)
      onChange(indexValues.toArray())
    }

    const toggleRegionOpen = (regionLabel: string) => {
      openedRegions.toggle(regionLabel)
    }

    return (
      <Menu anchorEl={anchorEl} open={open} onClose={onClose}>
        <MenuItem sx={combineSx(css.menuItem, css.menuItemRegion)} onClick={() => onSelectAll()}>
          <Checkbox indeterminate={someDepartmentsSelected && !allDepartmentsSelected} checked={allDepartmentsSelected} />
          {selectAllLabel}
        </MenuItem>
        {regions.map(region => {
          const allChecked = region.departments.every(_ => indexValues.has(_.code))
          const atLeastOneChecked = !!region.departments.find(_ => indexValues.has(_.code))
          return [
            <MenuItem
              sx={combineSx(css.menuItem, css.menuItemRegion)}
              key={region.label}
              onClick={() => onSelectDepartments(region.departments.map(_ => _.code))}
            >
              <Checkbox indeterminate={atLeastOneChecked && !allChecked} checked={allChecked} />
              <Box component="span" sx={css.regionLabel}>
                {region.label}
              </Box>
              <Icon
                sx={{
                  ...(openedRegions.has(region.label) && {color: t => t.palette.primary.main}),
                  ...css.regionToggleArrow,
                }}
                onClick={stopPropagation(() => toggleRegionOpen(region.label))}
              >
                {openedRegions.has(region.label) ? 'expand_less' : 'expand_more'}
              </Icon>
            </MenuItem>,
            openedRegions.has(region.label)
              ? region.departments.map(department => (
                  <MenuItem sx={css.menuItem} dense onClick={() => onSelectDepartment(department.code)}>
                    <Checkbox sx={css.cbDepartment} checked={indexValues.has(department.code)} />
                    <Box component="span" sx={{color: t => t.palette.text.disabled}}>
                      ({department.code})
                    </Box>
                    &nbsp;
                    {department.label}
                  </MenuItem>
                ))
              : [],
          ]
        })}
      </Menu>
    )
  },
)
