import { useCallback, useEffect, useState } from "react";
import { errorMessage } from "./api/client";

// Paginated orders loader that polls, so PENDING -> CONFIRMED shows up live.
export function usePagedOrders(fetcher, status, pollMs = 5000) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const res = await fetcher({ page, size: 10, ...(status ? { status } : {}) });
      setData(res.data.data);
      setError("");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [fetcher, page, status]);

  useEffect(() => { setPage(0); }, [status]);
  useEffect(() => {
    load();
    const t = setInterval(load, pollMs);
    return () => clearInterval(t);
  }, [load, pollMs]);

  return { page, setPage, data, error, loading, reload: load };
}
