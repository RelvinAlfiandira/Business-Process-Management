import { useState, useContext } from "react";
import { AuthContext } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "./RegisterPage.css";
import backgroundImage from "../../assets/images/background.png";
import ButtonCustom from "../../components/ui/button/ButtonCustom";

export default function RegisterPage() {
  const { register } = useContext(AuthContext);
  const navigate = useNavigate();

  const [form, setForm] = useState({ username: "", email: "", password: "" });

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
    
    if (!form.email.trim()) {
      toast.warning("Email harus diisi!");
      return;
    }
    
    // Validasi format email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(form.email)) {
      toast.warning("Format email tidak valid!");
      return;
    }
    
    if (!form.password.trim()) {
      toast.warning("Password harus diisi!");
      return;
    }
    
    const success = await register(form.username, form.email, form.password);
    if (success) {
      toast.success("Registrasi berhasil! Email selamat datang telah dikirim.");
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    }
  };

  return (
    <div className="register-container" style={{ backgroundImage: `linear-gradient(135deg, rgba(15, 15, 15, 0.7) 0%, rgba(26, 26, 26, 0.7) 100%), url(${backgroundImage})` }}>
      <div className="register-content">
      <div className="register-card">
        <h2 className="register-title">Register</h2>
        <p className="register-subtitle">Daftar untuk memulai perjalananmu</p>

        <form onSubmit={handleSubmit} className="register-form">
          <input
            className="register-input"
            name="username"
            placeholder="Username"
            onChange={handleChange}
          />
          <input
            className="register-input"
            name="email"
            placeholder="Email"
            onChange={handleChange}
          />
          <input
            className="register-input"
            name="password"
            type="password"
            placeholder="Password"
            onChange={handleChange}
          />
          <ButtonCustom
            type="submit"
            label="Register"
            variant="primary"
          />
        </form>

        <p className="register-footer">
          Sudah punya akun? <a href="/login">Login</a>
        </p>
      </div>
      
      <div className="register-right-content">
        <h1 className="register-right-title">Business Process Management</h1>
      </div>
      </div>
      <ToastContainer position="bottom-right" autoClose={3000} />
    </div>
  );
}
