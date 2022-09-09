import {Component, ReactNode} from 'react'
import {withToast, WithToast} from '../alexlibs/mui-extension'

interface Props extends WithToast {
  children: ReactNode
}

class Cp extends Component<Props> {
  componentDidCatch(error: Error | null, info: object) {
    const message = error ? error.message : JSON.stringify(info)
    this.props.toastError(message)
  }

  render() {
    return this.props.children
  }
}

export const ErrorBundary = withToast(Cp)
