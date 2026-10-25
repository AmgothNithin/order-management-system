import StatusBadge from "./StatusBadge";

const CANCELLABLE = ["PENDING", "CONFIRMED", "PAYMENT_FAILED", "PROCESSING"];
const fmt = (d) => (d ? new Date(d).toLocaleString() : "-");

export default function OrdersTable({ orders, showUser = false, onCancel }) {
  if (!orders.length) return <p className="muted">No orders found.</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Order</th>
            {showUser && <th>User</th>}
            <th>Items</th>
            <th>Total</th>
            <th>Status</th>
            <th>Created</th>
            {onCancel && <th></th>}
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td title={o.id}>#{o.id.slice(0, 8)}</td>
              {showUser && <td>{o.username}</td>}
              <td>{o.items.map((i) => `${i.productName} × ${i.quantity}`).join(", ")}</td>
              <td>₹{Number(o.totalAmount).toLocaleString()}</td>
              <td><StatusBadge status={o.status} /></td>
              <td>{fmt(o.createdAt)}</td>
              {onCancel && (
                <td>
                  {CANCELLABLE.includes(o.status) && (
                    <button className="btn-danger" onClick={() => onCancel(o.id)}>Cancel</button>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
