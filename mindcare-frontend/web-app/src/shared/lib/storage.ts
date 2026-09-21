const TOKEN_KEY = "mindcare.accessToken";
const USER_KEY = "mindcare.user";

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => {
    localStorage.setItem(TOKEN_KEY, token);
    window.dispatchEvent(new Event("mindcare:auth-changed"));
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    window.dispatchEvent(new Event("mindcare:auth-changed"));
  },
};

export const userStorage = {
  get: <T>() => {
    const value = localStorage.getItem(USER_KEY);
    if (!value) return null;

    try {
      return JSON.parse(value) as T;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  },
  set: <T>(user: T) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clear: () => localStorage.removeItem(USER_KEY),
};
