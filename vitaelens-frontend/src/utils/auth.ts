const TOKEN_KEY = 'vitaelens_token'
const USER_ID_KEY = 'vitaelens_userId'
const USERNAME_KEY = 'vitaelens_username'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearAuthStorage(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(USERNAME_KEY)
}

export { USER_ID_KEY, USERNAME_KEY }
