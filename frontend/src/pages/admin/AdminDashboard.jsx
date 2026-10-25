import { useState } from "react";
import { orderApi } from "../../api/client";
import { usePagedOrders } from "../../hooks";
import OrdersTable from "../../components/OrdersTable";
import Pagination from "../../components/Pagination";
import { STATUSES } from "../orders/OrderList";

export default function AdminDashboard() {
  const [status, setStatus] = useState("");
  const { page, setPage, data, error, loading } = usePagedOrders(orderApi.adminList, status);

  return (
    <div className="page">
      <div className="row between">
        <h2>All Orders</h2>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          {STATUSES.map((s) => <option key={s} value={s}>{s || "All statuses"}</option>)}
        </select>
      </div>
      <p className="muted">{data.totalElements ?? 0} orders · auto-refreshes every 5s</p>
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <p>Loading...</p> : <OrdersTable orders={data.content} showUser />}
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}
