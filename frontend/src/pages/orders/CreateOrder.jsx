import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { orderApi, productApi, errorMessage } from "../../api/client";

export default function CreateOrder() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]); // [{ productId, quantity }]
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    productApi.list()
      .then((r) => { setProducts(r.data.data); setProductId(r.data.data[0]?.id || ""); })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const byId = (id) => products.find((p) => p.id === id);
  const total = cart.reduce((s, i) => s + Number(byId(i.productId)?.price || 0) * i.quantity, 0);

  const addItem = () => {
    const qty = Number(quantity);
    if (!productId || !Number.isInteger(qty) || qty < 1) return;
    setCart((c) =>
      c.some((i) => i.productId === productId)
        ? c.map((i) => (i.productId === productId ? { ...i, quantity: i.quantity + qty } : i))
        : [...c, { productId, quantity: qty }]
    );
    setQuantity(1);
  };

  const placeOrder = async () => {
    setBusy(true);
    setError("");
    try {
      await orderApi.create(cart);
      navigate("/orders");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page">
      <h2>Create Order</h2>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="card">
        <div className="row">
          <select value={productId} onChange={(e) => setProductId(e.target.value)}>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} — ₹{Number(p.price).toLocaleString()} ({p.availableQuantity} in stock)
              </option>
            ))}
          </select>
          <input type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} className="qty" />
          <button className="btn-secondary" onClick={addItem}>Add</button>
        </div>
      </div>

      {cart.length > 0 && (
        <div className="card">
          <h3>Cart</h3>
          <table>
            <tbody>
              {cart.map((i) => (
                <tr key={i.productId}>
                  <td>{byId(i.productId)?.name}</td>
                  <td>× {i.quantity}</td>
                  <td>₹{(Number(byId(i.productId)?.price) * i.quantity).toLocaleString()}</td>
                  <td>
                    <button className="btn-link" onClick={() => setCart(cart.filter((c) => c.productId !== i.productId))}>
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="row between" style={{ marginTop: 12 }}>
            <strong>Total: ₹{total.toLocaleString()}</strong>
            <button className="btn-primary" disabled={busy} onClick={placeOrder}>
              {busy ? "Placing..." : "Place Order"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
