import { useState, useContext } from "react";
import { AuthContext } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "./LoginPage.css";
import backgroundImage from "../../assets/images/background.png";
import ButtonCustom from "../../components/ui/button/ButtonCustom";

export default function LoginPage() {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const [form, setForm] = useState({ username: "", password: "" });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validasi field kosong
    if (!form.username.trim()) {
      toast.warning("Username harus diisi!");
      return;
    }
    
    if (!form.password.trim()) {
      toast.warning("Password harus diisi!");
      return;
    }
    
    const success = await login(form.username, form.password);
    if (success) {
      navigate("/dashboard");
    }
  };

  return (
    <div className="login-container" style={{ backgroundImage: `linear-gradient(135deg, rgba(15, 15, 15, 0.7) 0%, rgba(26, 26, 26, 0.7) 100%), url(${backgroundImage})` }}>
      <div className="login-content">
      <div className="login-card">
        <h2 className="login-title">Login</h2>
        <p className="login-subtitle">Silakan masuk untuk melanjutkan</p>

        <form onSubmit={handleSubmit} className="login-form">
          <input
            className="login-input"
            name="username"
            placeholder="Username"
            onChange={handleChange}
          />
          <input
            className="login-input"
            name="password"
            type="password"
            placeholder="Password"
            onChange={handleChange}
          />
          <ButtonCustom
            type="submit"
            label="Login"
            variant="primary"
          />
        </form>

        <p className="login-footer">
          Belum punya akun? <a href="/register">Daftar</a>
        </p>
      </div>
      
      <div className="login-right-content">
        <h1 className="login-right-title">Business Process Management</h1>
      </div>
      </div>
      <ToastContainer position="bottom-right" autoClose={3000} />
    </div>
  );
}
