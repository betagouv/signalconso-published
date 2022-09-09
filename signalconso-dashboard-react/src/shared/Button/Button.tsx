import {Btn} from '../../alexlibs/mui-extension'
import {BtnProps} from '../../alexlibs/mui-extension'
import {forwardRef} from 'react'

export const ScButton = forwardRef((props: BtnProps, ref: any) => {
  return <Btn {...props} ref={ref} />
})
