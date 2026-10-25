import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;

  return (
    <nav className="navbar">
      <span className="brand">OMS</span>
      <div className="links">
        <NavLink to="/orders/new">New Order</NavLink>
        <NavLink to="/orders" end>My Orders</NavLink>
        {isAdmin && <NavLink to="/admin">Admin</NavLink>}
      </div>
      <div className="user">
        <span>{user.username}{isAdmin && " (admin)"}</span>
        <button className="btn-link" onClick={() => { logout(); navigate("/login"); }}>Logout</button>
      </div>
    </nav>
  );
}
