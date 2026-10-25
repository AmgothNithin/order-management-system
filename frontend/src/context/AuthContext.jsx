import { createContext, useContext, useState } from "react";
import { authApi } from "../api/client";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem("user")); } catch { return null; }
  });

  const persist = (data) => {
    localStorage.setItem("token", data.token);
    const u = { username: data.username, userId: data.userId, role: data.role };
    localStorage.setItem("user", JSON.stringify(u));
    setUser(u);
  };

  const login = async (body) => persist((await authApi.login(body)).data.data);
  const register = async (body) => persist((await authApi.register(body)).data.data);
  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isAdmin: user?.role === "ROLE_ADMIN", login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
