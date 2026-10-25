import axios from "axios";

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || "" });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Expired/invalid token -> clear session and go to login (auth endpoints excluded)
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.includes("/auth/");
    if (err.response?.status === 401 && !isAuthCall && localStorage.getItem("token")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

export const errorMessage = (err) => {
  const data = err.response?.data;
  if (data?.data && typeof data.data === "object") return Object.values(data.data).join(", ");
  if (err.response?.status === 403) return "You do not have permission to do that";
  return data?.message || (err.request ? "Cannot reach server" : err.message);
};

export const authApi = {
  login: (body) => api.post("/api/v1/auth/login", body),
  register: (body) => api.post("/api/v1/auth/register", body),
};

export const orderApi = {
  create: (items) => api.post("/api/v1/orders", { items }),
  list: (params) => api.get("/api/v1/orders", { params }),
  cancel: (id) => api.put(`/api/v1/orders/${id}/cancel`),
  adminList: (params) => api.get("/api/v1/orders/admin/all", { params }),
};

export const productApi = {
  list: () => api.get("/api/v1/inventory/products"),
};

export default api;
