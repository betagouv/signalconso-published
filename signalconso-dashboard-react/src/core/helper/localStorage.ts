export const localStorageObject = <T>(key: string) => ({
  set: (value: T): void => {
    localStorage.setItem(key, JSON.stringify(value))
  },

  get: (): T | undefined => {
    const item = localStorage.getItem(key)
    return item && JSON.parse(item)
  },

  clear: (): void => {
    localStorage.removeItem(key)
  },
})

export const localStorageString = (key: string) => ({
  set: (value: string): void => {
    localStorage.setItem(key, value)
  },

  get: (): string | undefined => {
    return localStorage.getItem(key) || undefined
  },

  clear: (): void => {
    localStorage.removeItem(key)
  },
})
