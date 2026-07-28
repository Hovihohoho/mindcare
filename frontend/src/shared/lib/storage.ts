const TOKEN_KEY = "mindcare.accessToken";
const USER_KEY = "mindcare.user";

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

export const userStorage = {
  get: <T>() => {
    const value = localStorage.getItem(USER_KEY);
    if (!value) return null;
    try { return JSON.parse(value) as T; } catch { return null; }
  },
  set: (user: unknown) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
};
