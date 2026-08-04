const TOKEN_KEY = "mindcare.accessToken";

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => {
    localStorage.setItem(TOKEN_KEY, token);
    window.dispatchEvent(new Event("mindcare:auth-changed"));
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    window.dispatchEvent(new Event("mindcare:auth-changed"));
  },
};
