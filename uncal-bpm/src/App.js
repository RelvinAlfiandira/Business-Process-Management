// src/App.js
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import Dashboard from "./pages/dashboard/Dashboard";
import PrivateRoute from "./components/routes/PrivateRoute";
import RegisterPage from "./pages/register/RegisterPage";
import LoginPage from "./pages/login/LoginPage";

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>

           {/* Default redirect */}
          <Route path="/" element={<Navigate to="/login" replace />} />

          {/* Public Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Private Routes */}
          <Route element={<PrivateRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            {/* contoh tambahan: */}
            {/* <Route path="/profile" element={<Profile />} /> */}
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
  );
}