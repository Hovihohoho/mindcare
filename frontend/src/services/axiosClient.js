import axios from 'axios';

const axiosClient = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080' });
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mindcare_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
axiosClient.interceptors.response.use(response => response, (error) => {
  if (error.response?.status === 401) {
    localStorage.removeItem('mindcare_token');
    localStorage.removeItem('mindcare_user');
    window.location.assign('/login');
  }
  return Promise.reject(error);
});
export default axiosClient;
