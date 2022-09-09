import {Box, Checkbox, Icon, Menu, MenuItem} from '@mui/material'
import * as React from 'react'
import {useEffect, useMemo} from 'react'
import {useSetState, UseSetState} from '../../alexlibs/react-hooks-lib'
import {useI18n} from '../../core/i18n'
import {makeSx} from '../../alexlibs/mui-extension'
import {combineSx, sxUtils} from '../../core/theme'
import {Company, CompanyWithAccessLevel} from '../../core/client/company/Company'

const css = makeSx({
  regionLabel: {
    fontWeight: t => t.typography.fontWeightBold,
    flex: 1,
  },
  regionToggleArrow: {
    width: 40,
    height: 36,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    ml: 1,
    borderRadius: t => t.shape.borderRadius + 'px',
    color: t => t.palette.text.disabled,
    '&:hover, &:active, &:focus': {
      background: t => t.palette.action.hover,
      color: t => t.palette.primary.main,
    },
  },
  locationIcon: {
    fontSize: 20,
  },
  menuItem: {
    pt: 0,
    pb: 0,
    pl: 1 / 2,
  },
  menuItemImportant: {
    fontWeight: t => t.typography.fontWeightBold,
    borderBottom: t => `1px solid ${t.palette.divider}`,
  },
  cbDepartment: {
    paddingTop: `6px !important`,
    paddingBottom: `6px !important`,
  },
})

interface SelectCompaniesProMenuProps {
  accessibleCompanies: CompanyWithAccessLevel[]
  onClose: () => void
  initialValues?: string[]
  anchorEl: HTMLElement | null
  open: boolean
  onChange: (departments: string[]) => void
}

export const SelectCompaniesByProMenu = ({
  onClose,
  initialValues,
  anchorEl,
  open,
  onChange,
  accessibleCompanies,
}: SelectCompaniesProMenuProps) => {
  const indexValues: UseSetState<string> = useSetState<string>()
  const {m} = useI18n()

  const allSelected = useMemo(() => indexValues.size === accessibleCompanies.length, [indexValues, accessibleCompanies])
  const someSelected = useMemo(
    () => !allSelected && !!accessibleCompanies.find(_ => indexValues.has(_.siret)),
    [allSelected, indexValues, accessibleCompanies],
  )

  useEffect(() => {
    indexValues.reset(initialValues)
  }, [])

  const onSelectAll = () => {
    accessibleCompanies.map(_ => _.siret).forEach(allSelected ? indexValues.delete : indexValues.add)
    onChange(indexValues.toArray())
  }

  const onSelect = (company: Company, index: number) => {
    indexValues.toggle(company.siret)
    onChange(indexValues.toArray())
  }

  return (
    <Menu anchorEl={anchorEl} open={open} onClose={onClose}>
      <MenuItem sx={combineSx(css.menuItem, css.menuItemImportant)} onClick={() => onSelectAll()}>
        <Checkbox indeterminate={someSelected} checked={allSelected} />
        {m.selectAll}
      </MenuItem>
      {accessibleCompanies.map((company, index) => (
        <MenuItem sx={css.menuItem} key={company.siret} dense onClick={() => onSelect(company, index)}>
          <Checkbox sx={css.cbDepartment} checked={indexValues.has(company.siret)} />
          <Box component="span" sx={{color: t => t.palette.text.secondary}}>
            {company.siret.slice(0, 9)}
          </Box>
          <Box component="span" sx={{fontWeight: t => t.typography.fontWeightBold}}>
            {company.siret.slice(9, 14)}
          </Box>
          <Box component="span" sx={{ml: 1, color: t => t.palette.text.disabled}}>
            <Icon sx={combineSx(sxUtils.inlineIcon, css.locationIcon)}>location_on</Icon>
            {company.address?.postalCode}
          </Box>
        </MenuItem>
      ))}
    </Menu>
  )
}
