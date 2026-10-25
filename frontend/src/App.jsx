import { Navigate, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import AuthPage from "./pages/auth/AuthPage";
import CreateOrder from "./pages/orders/CreateOrder";
import OrderList from "./pages/orders/OrderList";
import AdminDashboard from "./pages/admin/AdminDashboard";

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/login" element={<AuthPage mode="login" />} />
        <Route path="/register" element={<AuthPage mode="register" />} />
        <Route path="/orders" element={<ProtectedRoute><OrderList /></ProtectedRoute>} />
        <Route path="/orders/new" element={<ProtectedRoute><CreateOrder /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute adminOnly><AdminDashboard /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
    </>
  );
}
