export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button disabled={page === 0} onClick={() => onChange(page - 1)}>Prev</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}
