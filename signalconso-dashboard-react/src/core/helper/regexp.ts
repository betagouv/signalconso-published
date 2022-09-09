export const regexpPattern = {
  email: "^[a-zA-Z0-9_!#$'%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$",
  emailDGCCRF: '^[^@]+@[a-zA-Z0-9_\\-.]*\\.gouv\\.fr',
  siren: '[0-9]{9}',
  siret: '[0-9]{14}',
  activationCode: '[0-9]{6}',
  // https://stackoverflow.com/questions/12756159/regex-and-iso8601-formatted-datetime
  // Accepts formats :
  // YYYY-MM-DDThh:mm:ss
  // YYYY-MM-DDThh:mm:ssTZD
  // YYYY-MM-DDThh:mm:ss.sTZD
  isoDate: /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?(([+-]\d\d:\d\d)|Z)?$/i,
}

export const regexp = Object.entries(regexpPattern).reduce(
  (acc, [key, value]) => ({...acc, [key]: typeof value === 'string' ? new RegExp(value) : value}),
  {} as {[key in keyof typeof regexpPattern]: RegExp},
)
