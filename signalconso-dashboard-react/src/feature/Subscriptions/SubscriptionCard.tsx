import React, {CSSProperties, SyntheticEvent, useEffect, useMemo, useState} from 'react'
import {useI18n} from '../../core/i18n'
import {Panel, PanelHead} from '../../shared/Panel'
import {ScSelect} from '../../shared/Select/Select'
import {Chip, Collapse, duration, Icon} from '@mui/material'
import {SubscriptionCardRow} from './SubscriptionCardRow'
import {SelectCompanyDialog} from '../../shared/SelectCompany/SelectCompanyDialog'
import {ScChip} from '../../shared/Chip/ScChip'
import {ScChipContainer} from '../../shared/Chip/ScChipContainer'
import {useToast} from '../../core/toast'
import {SelectDepartmentsMenu} from '../../shared/SelectDepartments/SelectDepartmentsMenu'
import {SelectCountriesMenu} from '../../shared/SelectCountries/SelectCountriesMenu'
import {SelectMenu} from '../../shared/SelectMenu/SelectMenu'
import {IconBtn} from '../../alexlibs/mui-extension'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {ScMenuItem} from '../MenuItem/MenuItem'
import {SelectTagsMenu, SelectTagsMenuValues} from '../../shared/SelectTags/SelectTagsMenu'
import {Enum} from '../../alexlibs/ts-utils'
import {Subscription, SubscriptionCreate} from '../../core/client/subscription/Subscription'
import {ReportSearch} from '../../core/client/report/ReportSearch'
import {useConstantContext} from '../../core/context/ConstantContext'

interface Props {
  subscription: Subscription
  onUpdate: (_: Partial<SubscriptionCreate>) => Promise<Subscription>
  onDelete: (event: SyntheticEvent<any>) => void
  removing: boolean
  loading?: boolean
  className?: string
  style?: CSSProperties
}

const useAnchoredMenu = () => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)
  const open = (event: any) => setAnchorEl(event.currentTarget)
  const close = () => setAnchorEl(null)
  return {open, close, element: anchorEl}
}

export const SubscriptionCard = ({subscription, onUpdate, onDelete, removing, loading, className, style}: Props) => {
  const {m} = useI18n()
  const {toastInfo} = useToast()
  const departmentAnchor = useAnchoredMenu()
  const countriesAnchor = useAnchoredMenu()
  const categoryAnchor = useAnchoredMenu()
  const tagsAnchor = useAnchoredMenu()
  const _category = useConstantContext().categories
  const [isMounted, setIsMounted] = useState(false)

  useEffect(() => {
    _category.fetch({force: false})
    setIsMounted(true)
  }, [])

  const tags: SelectTagsMenuValues = useMemo(() => {
    const tags: SelectTagsMenuValues = {}
    subscription.withTags?.forEach(tag => {
      tags[tag] = 'included'
    })
    subscription.withoutTags?.forEach(tag => {
      tags[tag] = 'excluded'
    })
    return tags
  }, [subscription.withTags, subscription.withoutTags])

  const fromReportTagValues = (tags: SelectTagsMenuValues): Pick<ReportSearch, 'withTags' | 'withoutTags'> => {
    const res = {
      withTags: Enum.keys(tags).filter(tag => tags[tag] === 'included'),
      withoutTags: Enum.keys(tags).filter(tag => tags[tag] === 'excluded'),
    }
    return res
  }

  return (
    <Collapse in={isMounted} timeout={duration.standard * 1.5}>
      <Panel loading={loading} className={className} style={style}>
        <PanelHead
          sx={{
            display: 'flex',
            alignItems: 'center',
          }}
          action={
            <>
              <ScSelect
                sx={{mb: 0}}
                value={subscription.frequency ?? 'P7D'}
                onChange={(e: any) => onUpdate({frequency: e.target.value})}
              >
                <ScMenuItem value="P1D">{m.daily}</ScMenuItem>
                <ScMenuItem value="P7D">{m.weekly}</ScMenuItem>
              </ScSelect>
              &nbsp;
              <ScDialog title={m.removeSubscription} confirmLabel={m.delete} onConfirm={onDelete}>
                <IconBtn color="primary" loading={removing}>
                  <Icon>delete</Icon>
                </IconBtn>
              </ScDialog>
            </>
          }
        >
          {m.subscription}
        </PanelHead>

        <SubscriptionCardRow icon="flag" label={m.foreignCountry} onClick={countriesAnchor.open}>
          <ScChipContainer>
            {subscription.countries.map(_ => (
              <ScChip key={_.code} label={_.name} />
            ))}
          </ScChipContainer>
        </SubscriptionCardRow>
        <SelectCountriesMenu
          open={!!countriesAnchor.element}
          onChange={countries => onUpdate({countries})}
          initialValues={subscription.countries.map(_ => _.code)}
          anchorEl={countriesAnchor.element}
          onClose={countriesAnchor.close}
        />

        <SubscriptionCardRow icon="location_on" label={m.departments} onClick={departmentAnchor.open}>
          <ScChipContainer>
            {subscription.departments.map(_ => (
              <ScChip key={_.code} label={_.code + ' - ' + _.label} />
            ))}
          </ScChipContainer>
        </SubscriptionCardRow>
        <SelectDepartmentsMenu
          open={!!departmentAnchor.element}
          anchorEl={departmentAnchor.element}
          initialValues={subscription.departments.map(_ => _.code)}
          onClose={departmentAnchor.close}
          onChange={departments => onUpdate({departments})}
        />
        <SubscriptionCardRow icon="dashboard" label={m.categories} onClick={categoryAnchor.open}>
          <ScChipContainer>
            {subscription.categories.map(_ => (
              <ScChip key={_} label={_} />
            ))}
          </ScChipContainer>
        </SubscriptionCardRow>
        {_category.entity && (
          <SelectMenu
            multiple
            options={_category.entity}
            initialValue={subscription.categories}
            onClose={categoryAnchor.close}
            open={!!categoryAnchor.element}
            anchorEl={categoryAnchor.element}
            onChange={categories => onUpdate({categories})}
          />
        )}

        <SubscriptionCardRow icon="business" label={m.siret}>
          <ScChipContainer>
            {subscription.sirets.map(siret => (
              <ScChip
                key={siret}
                label={siret}
                onDelete={() => onUpdate({sirets: subscription.sirets.filter(_ => _ !== siret)})}
              />
            ))}
            <SelectCompanyDialog
              onChange={company => {
                if (!subscription.sirets.find(_ => _ === company.siret)) {
                  onUpdate({sirets: [...subscription.sirets, company.siret]})
                } else {
                  toastInfo(m.alreadySelectedCompany(company.name))
                }
              }}
            >
              <ScChip label={<Icon>add</Icon>} />
            </SelectCompanyDialog>
          </ScChipContainer>
        </SubscriptionCardRow>

        <SubscriptionCardRow icon="label" label={m.tags} onClick={tagsAnchor.open}>
          <ScChipContainer>
            {subscription?.withTags.map(_ => (
              <Chip key={_} color="success" icon={<Icon>add</Icon>} label={_} />
            ))}
            {subscription?.withoutTags.map(_ => (
              <Chip key={_} color="error" icon={<Icon>remove</Icon>} label={_} />
            ))}
          </ScChipContainer>
        </SubscriptionCardRow>
        <SelectTagsMenu
          onClose={tagsAnchor.close}
          value={tags}
          onChange={value => onUpdate(fromReportTagValues(value))}
          open={!!tagsAnchor.element}
          anchorEl={tagsAnchor.element}
        />
      </Panel>
    </Collapse>
  )
}
