import './polyfills'
import React from 'react'
import ReactDOM from 'react-dom'
import {App} from './App'
import createGenerateClassName from '@mui/styles/createGenerateClassName'
import StylesProvider from '@mui/styles/StylesProvider'
import {ErrorBundary} from './core/ErrorBundary'
import {Sentry} from './core/plugins/Sentry'

Sentry.init()

// https://github.com/mui-org/material-ui/issues/11843
// I think it should not be necessary. There is some miss configuration somewhere
const generateClassName = createGenerateClassName({
  productionPrefix: 'c',
  disableGlobal: true,
})

ReactDOM.render(
  <StylesProvider generateClassName={generateClassName}>
    <ErrorBundary>
      <App />
    </ErrorBundary>
  </StylesProvider>,
  document.getElementById('root'),
)
