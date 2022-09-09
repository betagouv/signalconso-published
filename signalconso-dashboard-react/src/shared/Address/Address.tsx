import React, {forwardRef} from 'react'
import {Address} from '../../core/model'

interface Props {
  address: Address
}

export const AddressComponent = forwardRef(({address}: Props, ref: any) => {
  return (
    <span ref={ref}>
      {address.number}&nbsp;
      {address.street}&nbsp;
      {address.addressSupplement}
      <br />
      {address.postalCode}&nbsp;
      {address.city}
      {address.country && (
        <>
          <br />
          {address.country}
        </>
      )}
    </span>
  )
})
