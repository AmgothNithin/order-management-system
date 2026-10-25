import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { errorMessage } from "../../api/client";

export default function AuthPage({ mode }) {
  const isRegister = mode === "register";
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/orders" replace />;

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (isRegister) await register(form);
      else await login({ username: form.username, password: form.password });
      navigate("/orders");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={submit}>
        <h2>{isRegister ? "Create account" : "Sign in"}</h2>
        {error && <div className="alert alert-error">{error}</div>}
        <label>Username<input value={form.username} onChange={set("username")} required minLength={3} /></label>
        {isRegister && (
          <label>Email<input type="email" value={form.email} onChange={set("email")} required /></label>
        )}
        <label>Password<input type="password" value={form.password} onChange={set("password")} required minLength={6} /></label>
        <button className="btn-primary" disabled={busy}>
          {busy ? "Please wait..." : isRegister ? "Register" : "Login"}
        </button>
        <p className="muted center">
          {isRegister
            ? <>Have an account? <Link to="/login">Login</Link></>
            : <>New here? <Link to="/register">Register</Link></>}
        </p>
      </form>
    </div>
  );
}
