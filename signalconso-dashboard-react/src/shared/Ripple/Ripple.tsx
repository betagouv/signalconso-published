import {ReactElement, useRef} from 'react'
import TouchRipple from '@mui/material/ButtonBase/TouchRipple'

interface Props {
  children: ReactElement<any>
}

export const Ripple = ({children}: Props) => {
  const rippleRef = useRef<any>(null)

  const onRippleStart = (e: any) => {
    rippleRef.current.start(e)
  }

  const onRippleStop = (e: any) => {
    rippleRef.current.stop(e)
  }

  return (
    <div onMouseDown={onRippleStart} onMouseLeave={onRippleStop} onMouseUp={onRippleStop} style={{position: 'relative'}}>
      {children}
      <TouchRipple ref={rippleRef} center={false} />
    </div>
  )
}
