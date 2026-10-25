import { useState } from "react";
import { orderApi, errorMessage } from "../../api/client";
import { usePagedOrders } from "../../hooks";
import OrdersTable from "../../components/OrdersTable";
import Pagination from "../../components/Pagination";

export const STATUSES = ["", "PENDING", "CONFIRMED", "PAYMENT_FAILED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"];

export default function OrderList() {
  const [status, setStatus] = useState("");
  const [actionError, setActionError] = useState("");
  const { page, setPage, data, error, loading, reload } = usePagedOrders(orderApi.list, status);

  const cancel = async (id) => {
    setActionError("");
    try { await orderApi.cancel(id); reload(); } catch (e) { setActionError(errorMessage(e)); }
  };

  return (
    <div className="page">
      <div className="row between">
        <h2>My Orders</h2>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          {STATUSES.map((s) => <option key={s} value={s}>{s || "All statuses"}</option>)}
        </select>
      </div>
      <p className="muted">Auto-refreshes every 5s</p>
      {(error || actionError) && <div className="alert alert-error">{error || actionError}</div>}
      {loading ? <p>Loading...</p> : <OrdersTable orders={data.content} onCancel={cancel} />}
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}
