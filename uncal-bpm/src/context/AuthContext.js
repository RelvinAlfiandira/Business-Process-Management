import { createContext, useState, useEffect } from "react";
import { toast } from "react-toastify";

export const AuthContext = createContext({
  token: null,
  user: null,
  login: async () => {},
  register: async () => {},
  logout: () => {},
  loading: true,
});

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // ✅ Rehydrate dari localStorage saat app dimuat
  useEffect(() => {
    const initAuth = async () => {
      const savedToken = localStorage.getItem("authToken");
      const savedUser = localStorage.getItem("authUser");

      if (savedToken) {
          setToken(savedToken);
        if (savedUser) setUser(JSON.parse(savedUser));
        try {
          // Validasi token ke backend
          const res = await fetch("http://localhost:8080/api/auth/verify-token", {
            headers: { Authorization: `Bearer ${savedToken}` },
          });

          if (!res.ok) {
            setToken(null);
            setUser(null);
            localStorage.removeItem("authToken");
            localStorage.removeItem("authUser");
          }
        } catch (err) {
          console.error("Error validasi token:", err);
        }
      }

      setTimeout(() => {
        setLoading(false);
      }, 2500);  // selesai loading
    };

    initAuth();
  }, []);

  // ✅ Simpan token & user ke localStorage setiap berubah
  useEffect(() => {
    if (token) {
      localStorage.setItem("authToken", token);
      localStorage.setItem("authUser", JSON.stringify(user));
    } else {
      localStorage.removeItem("authToken");
      localStorage.removeItem("authUser");
    }
  }, [token, user]);

  // ✅ REGISTER
  const register = async (username, email, password) => {
    try {
      const res = await fetch("http://localhost:8080/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, email, password }),
      });

      if (res.ok) {
        return true;
      } else {
        const contentType = res.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
          const errorData = await res.json();
          // Jika ada errors object (validasi field)
          if (errorData.errors) {
            const errorMessages = Object.values(errorData.errors).join(", ");
            toast.error(errorMessages);
          } else if (errorData.message) {
            toast.error(errorData.message);
          } else {
            toast.error("Registrasi gagal");
          }
        } else {
          const msg = await res.text();
          toast.error(msg);
        }
        return false;
      }
    } catch (err) {
      toast.error("Error koneksi backend: " + err.message);
      return false;
    }
  };

  // ✅ LOGIN
  const login = async (username, password) => {
    try {
      const res = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      if (res.ok) {
        const data = await res.json();
        setToken(data.token);
        setUser({ username }); // bisa diperluas kalau backend kirim email/role
        return true;
      } else {
        const msg = await res.text();
        toast.error("Login gagal: " + msg);
        return false;
      }
    } catch (err) {
      toast.error("Error koneksi backend: " + err.message);
      return false;
    }
  };

  // ✅ LOGOUT
  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem("authToken");
    localStorage.removeItem("authUser");
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        login,
        register,
        logout,
        loading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
